import java.net.*;
import java.text.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class ChatServer {
	
	JTextArea chatTextArea;
	HashMap<String, PrintWriter> clientWriters;
	HashMap<String, String> usernamesToPorts;
	
	public static void main (String[] args){
		ChatServer server = new ChatServer();
		server.initialiseServer();
	}
	
	void initialiseServer(){
		clientWriters = new HashMap<String, PrintWriter>();
		usernamesToPorts = new HashMap<String, String>();
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
				PrintWriter writer = (PrintWriter)pair.getValue();
				writer.println(message);
				writer.flush();
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
					PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
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
		BufferedReader reader;
		String username;
		String port;
		
		String[] msgHeaderEntries;
		String msgBody;
		
		boolean connected;
		
		public ClientInputHandler(Socket _clientSocket, ChatServer _server){
			try {
				clientSocket = _clientSocket;
				server = _server;
				InputStreamReader stream = new InputStreamReader(clientSocket.getInputStream());
				reader = new BufferedReader(stream);
				username = "unknown";
				port = "" + clientSocket.getPort();
				connected = true;
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}
		
		public void run(){
			String message;
			try{
				while (connected && (message = reader.readLine()) != null){
					parseMessage(message);
					handleMessageAction();
					Thread.sleep(10);
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}
		
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
		
		void handleMessageAction(){
			String msgType = msgHeaderEntries[0];
			String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
			
			switch (msgType){
			case "CMD_CONNECT":
				username = msgBody;
				synchronized (server){
					usernamesToPorts.put(username, port);
				}
				broadcastMessage(username + " has joined the server.");
				break;
				
			case "CMD_DISCONNECT":
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
				
			case "TRANS_CHAT":
				broadcastMessage(timeStamp + " " + username + " : " + msgBody);
				break;
				
			case "TRANS_FILE":
				break;
				
			case "CONT_ACK":
				break;
				
			case "CONT_RTREQ":
				break;
				
			default:
				break;
			}
		}
	}
	
}
