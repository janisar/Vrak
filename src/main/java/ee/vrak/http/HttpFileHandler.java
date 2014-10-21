package main.java.ee.vrak.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import main.java.ee.vrak.req.RequestGenerator;
import main.java.ee.vrak.resp.ResponseGenerator;
import main.java.ee.vrak.util.ClasspathUtil;
import main.java.ee.vrak.util.Constants;
import main.java.ee.vrak.util.JsonUtils;
import main.java.ee.vrak.util.ParameterUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class HttpFileHandler implements HttpRequestHandler {

	private File tempStoreFile;
	private File tempMachinesFile;
	private final String docRoot;
	private final String machinesFilePath;
	private int ttl = 4;
	private String command;

	public HttpFileHandler(File tempMachinesFile, File tempStoreFile,
			final String docRoot, int port) {
		super();
		this.tempStoreFile = tempStoreFile;
		this.tempMachinesFile = tempMachinesFile;
		Constants.port = port;
		this.docRoot = docRoot;
		this.machinesFilePath = docRoot.replace("index.html", "machines.txt");
	}

	@Override
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {
		InputStream machinesStream = ClasspathUtil
				.getFileFromClasspath(machinesFilePath);
		String jsonFileString = IOUtils.toString(machinesStream, "UTF8");
		boolean run = false, send = false, showModal = false;

		List<NameValuePair> params = ParameterUtil
				.getRequestParameters(request);

		for (NameValuePair nvp : params) {
			if (nvp.getName().toLowerCase().equals("proge")) {
				run = true;
				command = nvp.getValue();
			} else if (nvp.getName().toLowerCase().equals("data")) {
				updateResultView(nvp.getValue());
			} else if (nvp.getName().toLowerCase().equals("send")) {
				command = nvp.getValue();
				send = true;
			} else if (nvp.getName().toLowerCase().equals("returnip")) {
				Constants.returnIp = nvp.getValue();
			} else if (nvp.getName().toLowerCase().equals("returnport")) {
				Constants.returnPort = Integer.valueOf(nvp.getValue());
			} else if (nvp.getName().toLowerCase().equals("ttl")) {
				this.ttl = Integer.valueOf(nvp.getValue()) - 1;
			} else if (nvp.getName().toLowerCase().equals("clear")) {
				clearData();
			} else if (nvp.getName().toLowerCase().equals("getdata")) {
				sendFileToClient(response, nvp.getValue());
				showModal = true;
			}
		}
		if (run) {
			Map<String, ArrayList<String>> map = JsonUtils
					.getFriends(jsonFileString);
			new RequestGenerator(command, ttl).sendGetRequest(map);
			setHomeView(response);
		} else if (canSend(send)) {
			new ResponseGenerator(machinesFilePath, command, ttl)
			.setResponseJson(params);
		} else if (!showModal) {
			setHomeView(response);
		}
	}

	private boolean canSend(boolean send) {
		boolean notMe = Constants.ip != Constants.returnIp
				&& Constants.port != Constants.returnPort;
		return send && notMe;
	}

	private void clearData() throws IOException {
		FileUtils.write(tempStoreFile, "{\"data\":[]}");
	}

	private void updateResultView(String value) throws IOException {
		String data = JsonUtils.getData(value);
		File dataFile = new File("Data" + UUID.randomUUID().toString());
		FileUtils.write(dataFile, data);
		String dataWithFile = JsonUtils.replaceData(value,
				dataFile.getAbsolutePath());
		writeToFile(tempStoreFile, dataWithFile);
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

	private void setHomeView(final HttpResponse response) throws IOException {
		InputStream indexStream = ClasspathUtil.getFileFromClasspath(docRoot);
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

	private void sendFileToClient(HttpResponse response, String value) {

		response.setHeader("Content-Type", "text/html");
		value = URLDecoder.decode(value);
		File file = new File(value);

		try {
			String result = FileUtils.readFileToString(file);
			System.out.println(result);
			response.setEntity(new StringEntity(result, ContentType.create(
					"text/html", Charset.forName("UTF8"))));
			response.setHeader("Content-Length", "" + result.length());
		} catch (UnsupportedEncodingException e) {
		} catch (IOException e) {
			System.out.println("Cannot open file '" + file.getAbsolutePath()
					+ "'");
		}
	}

	public void setIp(String ip) {
		Constants.ip = ip;
	}

	public void setPort(int port) {
		Constants.port = port;
	}
}