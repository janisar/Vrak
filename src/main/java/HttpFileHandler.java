package main.java;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class HttpFileHandler implements HttpRequestHandler {

	private File tempStoreFile;
	private File tempMachinesFile;
	private final String docRoot;
	private final String jsonDocRoot;
	private int ttl = 4;
	private String ip;
	private int port;
	private String returnIp;
	private String executeProgram;
	private int returnPort;

	public HttpFileHandler(File tempMachinesFile, File tempStoreFile,
			final String docRoot, int port) {
		super();
		this.tempStoreFile = tempStoreFile;
		this.tempMachinesFile = tempMachinesFile;
		this.port = port;
		this.docRoot = docRoot;
		this.jsonDocRoot = docRoot.replace("index.html", "machines.txt");
	}

	InputStream getFileFromClasspath(String root) {
		return HttpFileHandler.class.getClassLoader().getResourceAsStream(root);
	}

	@Override
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {
		InputStream machinesStream = getFileFromClasspath(jsonDocRoot);
		String jsonFileString = IOUtils.toString(machinesStream, "UTF8");
		boolean run = false, send = false;
		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		List<NameValuePair> params = ParameterUtil.getRequestParameters(
				request, method);

		for (NameValuePair nvp : params) {
			if (nvp.getName().toLowerCase().equals("proge")) {
				run = true;
				executeProgram = nvp.getValue();
			} else if (nvp.getName().toLowerCase().equals("data")) {
				updateResultView(nvp.getValue());
			} else if (nvp.getName().toLowerCase().equals("send")) {
				executeProgram = nvp.getValue();
				send = true;
			} else if (nvp.getName().toLowerCase().equals("returnip")) {
				this.returnIp = nvp.getValue();
			} else if (nvp.getName().toLowerCase().equals("returnport")) {
				this.returnPort = Integer.valueOf(nvp.getValue());
			} else if (nvp.getName().toLowerCase().equals("ttl")) {
				this.ttl = Integer.valueOf(nvp.getValue()) - 1;
			} else if (nvp.getName().toLowerCase().equals("clear")) {
				clearData();
			} /*
			 * else if (nvp.getName().toLowerCase().equals("machine")) {
			 * JsonUtils.addNewMachine(tempMachinesFile, nvp.getValue()); }
			 */
		}
		if (run) {
			sendGetRequest(JsonUtils.getFriends(jsonFileString));
			setHomeView(response);
		} else if (send && this.ip != returnIp && this.port != returnPort) {
			setResponseJson(params);
		} else {
			setHomeView(response);
		}
	}

	private void clearData() throws IOException {
		FileUtils.write(tempStoreFile, "{\"data\":[]}");
	}

	private void updateResultView(String value) throws IOException {
		writeToFile(tempStoreFile, value);
	}

	private void writeToFile(File file, String result) throws IOException {
		RandomAccessFile randAccessFile = new RandomAccessFile(file, "rw");
		long fileLen = randAccessFile.length();
		randAccessFile.seek(fileLen - 2);
		if (fileLen > 20) {
			randAccessFile.write(",".getBytes());
		}
		randAccessFile.write(result.getBytes());
		randAccessFile.write("]}".getBytes());
		randAccessFile.close();
	}

	private void setResponseJson(List<NameValuePair> params) {
		String url = getResponseUrl();
		HttpPost post = new HttpPost(url);
		HttpClient client = HttpClientBuilder.create().build();
		ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(setMyResponseParameter());
		try {
			post.setEntity(new UrlEncodedFormEntity(postParameters));
			client.execute(post);
			if (ttl > 0) {
				String jsonFileString = IOUtils.toString(
						getFileFromClasspath(jsonDocRoot), "UTF8");
				sendGetRequest(JsonUtils.getFriends(jsonFileString));
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BasicNameValuePair setMyResponseParameter() {
		return new BasicNameValuePair("data", generateResponseJson());
	}

	private String generateResponseJson() {
		return JsonUtils.getResonseJson(ip, port, programRunningOnComputer());
	}

	/**
	 *
	 * @return
	 */
	private String programRunningOnComputer() {
		try {
			executeProgram = URLDecoder.decode(executeProgram, "UTF8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		try {
			if ("\\".equals(File.separator)) { // this guy is windosa
				System.out.println("Im Windows !!");
				p = rt.exec(executeProgram);
				p.waitFor();
			} else {
				p = rt.exec(new String[] { "bash", "-c", executeProgram });
			}
		} catch (IOException e) {
			System.out
					.println("Something bad happened while processing command '"
							+ executeProgram + "'");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String result = "Empty result";
		if (p != null) {
			InputStream stream = p.getInputStream();
			StringWriter writer = new StringWriter();
			try {
				IOUtils.copy(stream, writer, "UTF8");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(stream);
			}
			result = writer.toString();
		}
		return result;
	}

	private String getResponseUrl() {
		return "http://" + returnIp + ":" + returnPort;
	}

	private void setHomeView(final HttpResponse response) throws IOException {
		InputStream indexStream = getFileFromClasspath(docRoot);
		String machines = FileUtils.readFileToString(tempMachinesFile);
		if (indexStream.available() <= 0) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			StringEntity entity = new StringEntity(
					"<html><body><h1>File not found</h1></body></html>",
					ContentType.create("text/html", "UTF-8"));
			response.setEntity(entity);

		} else {
			response.setStatusCode(HttpStatus.SC_OK);
			String bodyString = IOUtils.toString(indexStream, "UTF8");
			String resultString = FileUtils.readFileToString(tempStoreFile);
			bodyString = bodyString.replace("${machines}", machines);
			bodyString = bodyString.replace("${result}", resultString);
			StringEntity body = new StringEntity(bodyString,
					ContentType.create("text/html", Charset.forName("UTF8")));
			response.setHeader("Content-Length", "" + body.getContentLength());
			response.setEntity(body);
		}
	}

	private void sendGetRequest(Map<String, ArrayList<String>> friendList)
			throws IllegalStateException, IOException {
		for (Map.Entry<String, ArrayList<String>> entry : friendList.entrySet()) {
			String ip = entry.getKey();
			ArrayList<String> portsOnThisIp = entry.getValue();

			for (String port : portsOnThisIp) {
				String url = getRequestUrl(ip, port);
				HttpGet get = new HttpGet(url);
				System.out.println(url);
				HttpClient client = HttpClientBuilder.create().build();
				try {
					client.execute(get);
				} catch (ClientProtocolException e) {
					System.out.println(url + " pole hetkel saadaval");
				} catch (IOException e) {
					System.out.println(url + " pole hetkel saadaval");
				}
			}
		}
	}

	private String getRequestUrl(String ip, String port) {
		String url = "http://" + ip + ":" + port + "?send=" + executeProgram;
		url += "&ttl=" + ttl;
		String urlEnd = (returnIp == null) ? "&returnip=" + getIpAddress()
				+ "&returnport=" + this.port : "&returnip=" + returnIp
				+ "&returnport=" + returnPort;
		url += urlEnd;
		return url;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public static String getIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& inetAddress instanceof Inet4Address) {
						String ipAddress = inetAddress.getHostAddress()
								.toString();
						return ipAddress;
					}
				}
			}
		} catch (SocketException ex) {
		}
		return null;
	}

	public void setPort(int port) {
		this.port = port;
	}
}