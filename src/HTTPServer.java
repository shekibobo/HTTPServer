import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Date;

public class HTTPServer {
	static String slash = File.separator;
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket;
		serverSocket = new ServerSocket(8080);
		
		// /home/user/public_html/
		File root = new File(System.getProperty("user.home") + slash + "public_html");
		while (true) {	// boot the server
			Date modifiedRequest = new Date(0);
			String response;
			Socket connectionSocket = serverSocket.accept();
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(
							connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(
					connectionSocket.getOutputStream());
			String filetype = "text/html";
			try {
				if (inFromClient.ready()) {
					
					String in = inFromClient.readLine();
					System.out.println(in);
					String requestFilename = getFileRequest(in);
					if (requestFilename.matches(".*(.jpg)")) {
						filetype = "image/jpeg";
					}
					else if (requestFilename.matches(".*(.png)")) {
						filetype = "image/png";
					}
					while (inFromClient.ready()) {
						String line = inFromClient.readLine();
						System.out.println(line);
						if (line.matches("(If-modified-since:).*")) {
							modifiedRequest = new Date(line.replace("If-modified-since: ", ""));
						}
						else modifiedRequest = new Date(0);
					}

					File requestFile = new File(root + requestFilename);
					Date lastModified = new Date(requestFile.lastModified());
					if (requestFile.exists()) response = setResponse("200 OK", filetype);
					else throw new HTTPHeaderException(404, "File not found");
					System.out.println(requestFile);
					if (lastModified.after(modifiedRequest)) {
						System.out.println(response);
						sendResponse(outToClient, response, requestFile);
					}
				}
			}
			catch (HTTPHeaderException e) {
				response = setResponse(e.toString(), filetype);
				sendResponse(outToClient, response, null);
			}
		}
	}

	private static String getFileRequest(String headerLine) throws HTTPHeaderException {
		String[] headerTokens = headerLine.split(" ");
		for (String s : headerTokens) {
			if (s.matches("/.*(.html)?")) {
				if (s.matches(".*/")) return s.replace("/", slash) + "index.html";
				else return s.replace("/", slash);
			}
		}		
		// throw bad request exception
		throw new HTTPHeaderException(400, "Bad Request");
	}
	
	private static String setResponse(String code, String filetype) {
		Date d = new Date();
		String response = "HTTP/1.1 " + code + "\r\n";
		response += "Date: " + d.toString() + "\r\n";
		response += "Server: HTTPServer/0.0.3\r\n";
		response += "Connection: keep-alive\r\n";
		response += "Content-Type: " + filetype + " charset=UTF-8\r\n";
		response += "\r\n";
		return response;
	}
	
	private static void sendResponse(DataOutputStream outToClient, String response, File file)
		throws IOException {
		byte[] fileBytes = new byte[0];
		if (file != null) {
			System.out.println("sending file: " + file.toString());
			fileBytes = new byte[(int)file.length()];
			BufferedInputStream fileBuffer = new BufferedInputStream(new FileInputStream(file));
			fileBuffer.read(fileBytes);
		} else {
			String fof = "<h1>404 File not found</h1>";
			fileBytes = fof.getBytes();
		}
		
		outToClient.write(response.getBytes());		
		outToClient.write(fileBytes);
		outToClient.close();
		System.out.println("Connection closed.");
	}
}


