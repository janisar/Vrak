package main.java.ee.vrak.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonUtils {

	public static Map<String, ArrayList<String>> getMachines(String jsonString) {
		Map<String, ArrayList<String>> machinesMap = new HashMap<String, ArrayList<String>>();
		JSONArray object = new JSONArray();
		JSONParser parser = new JSONParser();
		try {
			object = (JSONArray) parser.parse(jsonString);
		} catch (ParseException e) {
		}
		for (Object k : object) {
			JSONArray current = (JSONArray) k;
			String ip = current.get(0).toString();
			String port = current.get(1).toString();
			System.out.println(ip + ":" + port);
			if (!machinesMap.containsKey(ip)) {
				machinesMap.put(ip, new ArrayList<String>());
			}
			machinesMap.get(ip).add(port);
		}
		return machinesMap;
	}

	@SuppressWarnings("unchecked")
	public static String getResonseJson(String ip, int port, String resultBlob) {
		resultBlob = escapeSlash(resultBlob);
		/*
		 * if (resultBlob.length() > 20) { resultBlob =
		 * resultInByteArrayFile(resultBlob).toString(); }
		 */
		String opSys = System.getProperty("os.name");
		if (opSys.contains("Windows")) {
			opSys = "Windows";
		}
		ip = ip.substring(1);
		JSONObject json = new JSONObject();
		json.put("ip", ip);
		json.put("port", port);
		json.put("op", opSys);
		json.put("date", new Date().toString());
		json.put("data", resultBlob);
		return json.toJSONString();
	}

	private static String escapeSlash(String resultBlob) {

		String a = resultBlob.replaceAll("\\n", "<br/>");
		a = a.replaceAll("\\r", " ");
		return a;
	}

	private static byte[] resultInByteArrayFile(String resultBlob) {
		byte[] result = null;
		try {
			result = resultBlob.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static void addNewMachine(File tempMachinesFile, String machine)
			throws IOException {
		JSONObject newMachine = new JSONObject();
		newMachine.put("ip", machine.split(" ")[0]);
		newMachine.put("port", machine.split(" ")[1]);
		String currentMachines = FileUtils.readFileToString(tempMachinesFile);
		JSONParser parser = new JSONParser();
		try {
			JSONObject object = (JSONObject) parser.parse(currentMachines);
			object.put("friend", newMachine);
			FileUtils.write(tempMachinesFile, object.toJSONString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public static String getData(String value) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject object = (JSONObject) parser.parse(value);
			return object.get("data").toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String replaceData(String value, String absolutePath) {
		JSONParser parser = new JSONParser();
		JSONObject object = null;
		try {
			object = (JSONObject) parser.parse(value);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		JSONObject json = new JSONObject();
		json.put("ip", object.get("ip"));
		json.put("port", object.get("port"));
		json.put("op", object.get("op"));
		json.put("date", object.get("date"));
		json.put("data", absolutePath);
		return json.toJSONString();
	}
}
