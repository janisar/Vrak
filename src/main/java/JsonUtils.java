package main.java;

import java.io.File;
import java.io.IOException;
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

	public static Map<String, ArrayList<String>> getFriends(String jsonString) {
		JSONParser parser = new JSONParser();
		Map<String, ArrayList<String>> friendMap = new HashMap<String, ArrayList<String>>();
		try {
			JSONObject object = (JSONObject) parser.parse(jsonString);
			JSONArray friendsArray = (JSONArray) object.get("friends");
			Object[] list = friendsArray.toArray();
			for (Object o : list) {
				JSONObject friend = (JSONObject) parser.parse(o.toString());
				if (!friendMap.containsKey(friend.get("ip"))) {
					friendMap.put(friend.get("ip").toString(),
							new ArrayList<String>());
				}
				friendMap.get(friend.get("ip").toString()).add(
						friend.get("port").toString());
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return friendMap;
	}

	@SuppressWarnings("unchecked")
	public static String getResonseJson(String ip, int port, String resultBlob) {
		resultBlob = escapeSlash(resultBlob);
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
		String a = resultBlob.replaceAll("\\n", "\\\\n");
		a = a.replaceAll("\\r", "\\\\r");
		return a;
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
}
