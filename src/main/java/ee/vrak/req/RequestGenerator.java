package main.java.ee.vrak.req;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import main.java.ee.vrak.util.Constants;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class RequestGenerator {

	String command;
	int ttl;

	public RequestGenerator(String command, int ttl) {
		this.command = command;
		this.ttl = ttl;
	}

	public void sendGetRequest(Map<String, ArrayList<String>> friendList)
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
		String url = "http://" + ip + ":" + port + "?send=" + command;
		url += "&ttl=" + ttl;
		String urlEnd = (Constants.returnIp == null) ? "&returnip="
				+ getIpAddress() + "&returnport=" + Constants.port
				: "&returnip=" + Constants.returnIp + "&returnport="
						+ Constants.returnPort;
		url += urlEnd;
		return url;
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
}
