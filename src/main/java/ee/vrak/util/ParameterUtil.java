package main.java.ee.vrak.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public class ParameterUtil {
	public static List<NameValuePair> getRequestParameters(HttpRequest request)
			throws MethodNotSupportedException, IOException {
		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		List<NameValuePair> result = new ArrayList<NameValuePair>();
		if (!method.equals("GET") && !method.equals("POST")) {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		} else if (method.equals("GET")) {
			result = getGetParameters(request.getRequestLine().getUri());
		} else {
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntity requestEntity = ((HttpEntityEnclosingRequest) request)
						.getEntity();
				result = URLEncodedUtils.parse(requestEntity);
			}
		}
		return result;
	}

	/**
	 * Võtab urli ette ja tükeldab parameetrid endale mõistetavaks.
	 * NameValuePair on tore asi.
	 *
	 * @param uri
	 * @return
	 */
	private static List<NameValuePair> getGetParameters(String uri) {
		if (uri.length() > 2) {
			uri = uri.substring(2);
		}
		List<NameValuePair> result = new ArrayList<NameValuePair>();
		String[] splitParams = uri.split("&");
		for (String s : splitParams) {
			String[] keyValuePair = s.split("=");
			if (keyValuePair.length > 1) {
				for (int i = 0; i < keyValuePair.length; i += 2) {
					result.add(new BasicNameValuePair(keyValuePair[0],
							keyValuePair[1]));
				}
			}
		}
		return result;
	}
}
