package main.java;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

public class Server {

	public static int PORT = 14356;

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			try {
				PORT = Integer.valueOf(args[0]);
			} catch (NumberFormatException ex) {
				System.out.println("Only port parameter is optional");
			}
		}
		HttpProcessor httpProc = HttpProcessorBuilder.create().build();
		UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
		String docRoot = "main/resources/index.html";

		File tempStoreFile = File.createTempFile("P2Net_data", ".txt");
		File tempMachinesFile = File.createTempFile("P2Net_machines", ".txt");
		initFiles(tempStoreFile, tempMachinesFile);
		HttpFileHandler httpFileHandler = new HttpFileHandler(tempMachinesFile,
				tempStoreFile, docRoot, PORT);
		registry.register("*", httpFileHandler);

		HttpService service = new HttpService(httpProc, registry);

		Thread t = new RequestListenerThread(PORT, service, httpFileHandler);
		t.setDaemon(false);
		t.start();
		tempStoreFile.deleteOnExit();
	}

	private static void initFiles(File tempStoreFile, File tempMachinesFile) {
		InputStream is = Server.class.getClassLoader().getResourceAsStream(
				"main/resources/machines.txt");
		try {
			String currentMachines = IOUtils.toString(is);
			FileUtils.write(tempStoreFile, "{\"data\":[]}");
			FileUtils.write(tempMachinesFile, currentMachines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class RequestListenerThread extends Thread {

		private HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
		private ServerSocket serversocket;
		private HttpService httpService;
		private HttpFileHandler httpFileHandler;

		public RequestListenerThread(int port, HttpService httpService,
				HttpFileHandler httpFileHandler) throws IOException {
			connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
			serversocket = new ServerSocket(port);
			this.httpService = httpService;
			this.httpFileHandler = httpFileHandler;
		}

		@Override
		public void run() {
			System.out.println("Listening on port "
					+ serversocket.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					Socket socket = serversocket.accept();
					System.out.println("Incoming connection from "
							+ socket.getInetAddress());
					HttpServerConnection conn = connFactory
							.createConnection(socket);
					httpFileHandler.setIp(socket.getLocalAddress().toString());
					// Start worker thread
					Thread t = new WorkerThread(httpService, conn);
					t.setDaemon(true);
					t.start();
				} catch (InterruptedIOException ex) {
					break;
				} catch (IOException e) {
					System.err
							.println("I/O error initialising connection thread:"
									+ e.getMessage());
					break;
				} finally {
					System.out.println("Connection closed");
				}
			}
		}
	}

	static class WorkerThread extends Thread {

		private HttpService httpservice;
		private HttpServerConnection conn;

		public WorkerThread(HttpService httpservice, HttpServerConnection conn) {
			super();
			this.httpservice = httpservice;
			this.conn = conn;
		}

		@Override
		public void run() {
			HttpContext context = new BasicHttpContext(null);
			try {
				while (!Thread.interrupted() && conn.isOpen()) {
					httpservice.handleRequest(conn, context);
				}
			} catch (ConnectionClosedException ex) {
				System.err.println("Client closed connection");
			} catch (IOException ex) {
				System.err.println("I/O error: " + ex.getMessage());
			} catch (HttpException ex) {
				System.err.println("Unrecoverable HTTP protocol violation: "
						+ ex.getMessage());
			} finally {
				try {
					conn.shutdown();
				} catch (Exception ignore) {
				}
			}

		}
	}
}
