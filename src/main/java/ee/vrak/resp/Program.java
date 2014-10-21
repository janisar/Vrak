package main.java.ee.vrak.resp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.io.IOUtils;

public class Program {

	String command;

	public Program(String command) {
		try {
			this.command = URLDecoder.decode(command, "UTF8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Something went wrong while decoding command '"
					+ command + "'");
		}
	}

	public String runCommandOnComputer() {
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		try {
			if (isWindows()) {
				p = rt.exec(command);
				p.waitFor();
			} else {
				p = rt.exec(new String[] { "bash", "-c", command });
			}
		} catch (IOException e) {
			System.out
					.println("Something bad happened while processing command '"
							+ command + "'");
		} catch (InterruptedException e) {
		}
		return processStreamToString(p);
	}

	private boolean isWindows() {
		return "\\".equals(File.separator);
	}

	private String processStreamToString(Process p) {
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
}
