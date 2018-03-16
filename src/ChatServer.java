import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class ChatServer {
	
	ArrayList<PrintWriter> clientOutputStreams;
	boolean serverOnline;
	
	JTextArea incoming;
	
	public static void main (String[] args){
		ChatServer server = new ChatServer();
		server.initialiseServer();
	}
	
	void initialiseServer(){
		clientOutputStreams = new ArrayList<PrintWriter>();
		setUpGUI();
		Thread t = new Thread(new ConnectionRequestListener(6666, this));
		t.start();
	}
	
	void setUpGUI(){
		JFrame frame = new JFrame("Chat Server");
		JPanel mainPanel = new JPanel();
		JLabel heading = new JLabel("CHAT SERVER");
		incoming = new JTextArea(22, 50);
		incoming.setLineWrap(true);
		incoming.setWrapStyleWord(true);
		incoming.setEditable(false);
		JScrollPane qScroller = new JScrollPane(incoming);
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
	public void broadcastMessage(String message){
		Iterator<PrintWriter> it = clientOutputStreams.iterator();
		while (it.hasNext()){
			try {
				PrintWriter writer = it.next();
				writer.println(message);
				writer.flush();
				incoming.append(message + "\n");
			} catch (Exception ex) {
				System.out.println(ex);
			}
		}
	}
	
	// used by only one thread, to listen for new connection requests
	// creates a new input handler thread and output stream for each new connected client
	public class ConnectionRequestListener implements Runnable {
		ServerSocket serverSock;
		ChatServer server;
		
		public ConnectionRequestListener(int portNo, ChatServer _server){
			try {
				serverSock = new ServerSocket(6666);
				server = _server;
			} catch (Exception ex) {
				System.out.println(ex);
			}
		}
		
		public void run(){
			try {
				while (true){
					Socket clientSock = serverSock.accept();
					PrintWriter writer = new PrintWriter(clientSock.getOutputStream());
					server.clientOutputStreams.add(writer);
					Thread t = new Thread(new ClientInputHandler(clientSock));
					t.start();
					Thread.sleep(10);
				}
			} catch (Exception ex) {
				System.out.println(ex);
			}
		}
	}
	
	// used by one thread for each client, to receive messages from that client and broadcast to all
	public class ClientInputHandler implements Runnable {
		BufferedReader reader;
		Socket sock;
		
		public ClientInputHandler(Socket clientSock){
			try {
				sock = clientSock;
				InputStreamReader stream = new InputStreamReader(sock.getInputStream());
				reader = new BufferedReader(stream);
			} catch (Exception ex){
				System.out.println(ex);
			}
		}
		
		public void run(){
			String message;
			try{
				while ((message = reader.readLine()) != null){
					broadcastMessage(message);
					Thread.sleep(10);
				}
			} catch (Exception ex){
				System.out.println(ex);
			}
		}
	}
	
}
