package main.java.ee.vrak.req;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import main.java.ee.vrak.util.Constants;
import main.java.ee.vrak.util.JsonUtils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class RequestGenerator {

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
	String command;

	int ttl;

	public RequestGenerator() {
	}

	public RequestGenerator(String command, int ttl) {
		this.command = command;
		this.ttl = ttl;
	}

	private void addMachinesToMap(Map<String, ArrayList<String>> map,
			String machines) {
		Map<String, ArrayList<String>> currentMachines = JsonUtils
				.getFriends(machines);
		for (Entry<String, ArrayList<String>> entry : currentMachines
				.entrySet()) {
			String ip = entry.getKey();
			if (!map.containsKey(ip)) {
				map.put(ip, new ArrayList<String>());
			}
			for (String port : entry.getValue()) {
				map.get(ip).add(port);
			}
		}

	}

	public void getDijkstraMachines(Map<String, ArrayList<String>> map,
			String url) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);

		try {
			HttpResponse response = client.execute(get);
			String machines = getResponseContent(response);
			addMachinesToMap(map, machines);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	private String getRequestUrl(String ip, String port) {
		String url = "http://" + ip + ":" + port + "/run?prog=" + command;
		url += "&ttl=" + ttl;
		String urlEnd = (Constants.sendIp == null) ? "&sendip="
				+ getIpAddress() + "&sendport=" + Constants.port : "&sendip="
				+ Constants.sendIp + "&sendport=" + Constants.sendPort;
		url += urlEnd;
		return url;
	}

	private String getResponseContent(HttpResponse response) throws IOException {
		InputStream is = response.getEntity().getContent();
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer);
		StringBuffer sb = writer.getBuffer();
		String machines = sb.toString();
		return machines;
	}

	private boolean notMe(String ip, String port) {
		if (Constants.ip.equals(ip) && Constants.port != Integer.valueOf(port)) {
			return false;
		}
		return true;
	}

	public void sendGetRequest(Map<String, ArrayList<String>> friendList)
			throws IllegalStateException, IOException {
		for (Map.Entry<String, ArrayList<String>> entry : friendList.entrySet()) {
			String ip = entry.getKey();
			ArrayList<String> portsOnThisIp = entry.getValue();

			for (String port : portsOnThisIp) {
				String url = getRequestUrl(ip, port);
				HttpGet get = new HttpGet(url);
				System.out.println("Sending GET request to '" + url + "'");
				HttpClient client = HttpClientBuilder.create().build();
				try {
					if (notMe(ip, port)) {
						client.execute(get);
					}
				} catch (ClientProtocolException e) {
					System.out.println(url + " pole hetkel saadaval");
				} catch (IOException e) {
					System.out.println(url + " pole hetkel saadaval");
				}
			}
		}
	}
}
