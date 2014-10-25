package main.java.ee.vrak.resp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import main.java.ee.vrak.req.RequestGenerator;
import main.java.ee.vrak.util.ClasspathUtil;
import main.java.ee.vrak.util.Constants;
import main.java.ee.vrak.util.JsonUtils;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class ResponseGenerator {

	int ttl;
	String command;
	String machinesFilePath;

	public ResponseGenerator(String machinesFilePath, String command, int ttl) {
		this.machinesFilePath = machinesFilePath;
		this.command = command;
		this.ttl = ttl;
	}

	private String generateResponseJson() {
		String commandResult = new Program(command).runCommandOnComputer();
		return JsonUtils.getResonseJson(Constants.ip, Constants.port,
				commandResult);
	}

	private String getResponseUrl() {
		return "http://" + Constants.sendIp + ":" + Constants.sendPort + "/result";
	}

	private StringEntity setMyResponseParameter() {
		StringEntity input = null;
		String data = generateResponseJson();
		try {
			input = new StringEntity(data);
			input.setContentType("application/json");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return input;
	}

	public void setResponseJson(List<NameValuePair> params) {
		String url = getResponseUrl();
		HttpPost post = new HttpPost(url);
		HttpClient client = HttpClientBuilder.create().build();
		StringEntity postParameters = setMyResponseParameter();
		try {
			post.setEntity(postParameters);
			client.execute(post);
			if (ttl > 0) {
				String jsonFileString = IOUtils.toString(
						ClasspathUtil.getFileFromClasspath(machinesFilePath),
						"UTF8");
				Map<String, ArrayList<String>> map = JsonUtils
						.getFriends(jsonFileString);
				new RequestGenerator(command, ttl).sendGetRequest(map);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
