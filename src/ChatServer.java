import java.net.*;
import java.text.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class ChatServer {
	
	JTextArea chatTextArea;
	
	HashMap<String, ObjectOutputStream> clientWriters; // <port, output stream>
	HashMap<String, String> usernamesToPorts; // <username, port>
	
	byte[] sharedFileData;
	String sharedFileName;
	
	public static void main (String[] args){
		ChatServer server = new ChatServer();
		server.initialiseServer();
	}
	
	void initialiseServer(){
		clientWriters = new HashMap<String, ObjectOutputStream>();
		usernamesToPorts = new HashMap<String, String>();
		sharedFileData = null;
		setUpGUI();
		Thread t = new Thread(new ConnectionRequestListener(6666, this));
		t.start();
	}
	
	void setUpGUI(){
		JFrame frame = new JFrame("Chat Server");
		JPanel mainPanel = new JPanel();
		JLabel heading = new JLabel("CHAT SERVER");
		chatTextArea = new JTextArea(22, 50);
		chatTextArea.setLineWrap(true);
		chatTextArea.setWrapStyleWord(true);
		chatTextArea.setEditable(false);
		JScrollPane qScroller = new JScrollPane(chatTextArea);
		qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		mainPanel.add(heading);
		mainPanel.add(qScroller);
		frame.getContentPane().add(BorderLayout.CENTER,  mainPanel);
		frame.setSize(600,  440);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	// broadcasts a message to all connected clients
	synchronized void broadcastMessage(String message){
		chatTextArea.append(message + "\n");
		@SuppressWarnings("rawtypes")
		Iterator it = clientWriters.entrySet().iterator();
		while (it.hasNext()){
			try {
				@SuppressWarnings("rawtypes")
				Map.Entry pair = (Map.Entry)it.next();
				ObjectOutputStream writer = (ObjectOutputStream)pair.getValue();
				writer.writeObject(message);
				writer.flush();
				Thread.sleep(10);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	// sends a message to one individual client
	synchronized void directMessage(Object message, String port){
		if (clientWriters.containsKey(port)){
			try {
				ObjectOutputStream writer = clientWriters.get(port);
				writer.writeObject(message);
				writer.flush();
				Thread.sleep(10);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	// used by only one thread, to listen for new connection requests
	// creates a new input handler thread and output stream for each new connected client
	public class ConnectionRequestListener implements Runnable {
		ServerSocket serverSocket;
		ChatServer server;
		
		public ConnectionRequestListener(int portNo, ChatServer _server){
			try {
				serverSocket = new ServerSocket(6666);
				server = _server;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		public void run(){
			try {
				while (true){
					Socket clientSocket = serverSocket.accept();
					ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
					synchronized (server){
						server.clientWriters.put("" + clientSocket.getPort(), writer);
					}
					Thread t = new Thread(new ClientInputHandler(clientSocket, server));
					t.start();
					Thread.sleep(10);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	// used by one thread for each client, to receive and process messages from that client
	public class ClientInputHandler implements Runnable {
		Socket clientSocket;
		ChatServer server;
		ObjectInputStream reader;
		String username;
		String port;
		
		String[] msgHeaderEntries;
		String msgBody;
		
		boolean connected;
		boolean expectingFile;
		
		public ClientInputHandler(Socket _clientSocket, ChatServer _server){
			try {
				clientSocket = _clientSocket;
				server = _server;
				reader = new ObjectInputStream(clientSocket.getInputStream());
				username = "unknown";
				port = "" + clientSocket.getPort();
				connected = true;
				expectingFile = false;
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}
		
		// receives and processes chat messages and commands as well as shared file data
		public void run(){
			Object message;
			try {
				while (connected && (message = reader.readObject()) != null){
					if (expectingFile){
						expectingFile = false;
						synchronized (server){
							sharedFileData = (byte[]) message;
						}
						broadcastMessage(username + " has shared file " + sharedFileName + ". Type /accept to download.");
					} else {
						parseMessage((String)message);
						handleMessageAction();
					}
					Thread.sleep(10);
				}
			} catch (Exception ex){
				//ex.printStackTrace();
			}
		}
		
		// separate non-file data messages into header (message type and arguments) and body
		void parseMessage(String message){
			String[] headerAndBody = message.split("\\|");
			msgHeaderEntries = headerAndBody[0].trim().split("\\s++");
			msgBody = "";
			if (headerAndBody.length > 0){
				msgBody = headerAndBody[1];
				for (int i = 1; i < headerAndBody.length; i++){
					if ((i+1) < headerAndBody.length){
						msgBody += "|" + headerAndBody[i+1];
					}
				}
			}
			msgBody = msgBody.trim();
		}
		
		// take appropriate action depending on message header content
		void handleMessageAction(){
			String msgType = msgHeaderEntries[0];
			String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
			
			switch (msgType){
			case "CONNECT":
				username = msgBody;
				synchronized (server){
					usernamesToPorts.put(username, port);
				}
				broadcastMessage(username + " has joined the server.");
				break;
				
			case "DISCONNECT":
				broadcastMessage(username + " has left the server.");
				try {
					connected = false;
					synchronized (server){
						clientWriters.remove(port);
						usernamesToPorts.remove(username);
					}
					reader.close();
					clientSocket.close();
					} catch (IOException ex){
						ex.printStackTrace();
					}
				break;
				
			case "CHAT":
				broadcastMessage(timeStamp + " " + username + " : " + msgBody);
				break;
				
			case "SHARE_FILE":
				expectingFile = true;
				String[] filePath = msgBody.split("/");
				synchronized (server){
					sharedFileName = filePath[filePath.length-1];
				}
				break;
				
			case "DL_FILE":
				if (sharedFileData != null){
					try {
						directMessage(sharedFileName, port);
						Thread.sleep(100);
						directMessage(sharedFileData, port);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				} else {
					directMessage("", port);
				}
				break;
				
			default:
				directMessage("< SERVER: INVALID MESSAGE FORMAT >", port);
				break;
			}
		}
	}
	
}
