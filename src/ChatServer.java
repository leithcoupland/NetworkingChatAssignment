import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {
	
	ArrayList<PrintWriter> clientOutputStreams;
	
	public static void main (String[] args){
		ChatServer server = new ChatServer();
		server.initialiseServer();
	}
	
	void initialiseServer(){
		clientOutputStreams = new ArrayList<PrintWriter>();
		Thread t = new Thread(new ConnectionRequestListener(6666, this));
		t.start();
	}
	
	// broadcasts a message to all connected clients
	public void broadcastMessage(String message){
		Iterator<PrintWriter> it = clientOutputStreams.iterator();
		while (it.hasNext()){
			try {
				PrintWriter writer = it.next();
				writer.println(message);
				writer.flush();
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
					//System.out.println("read " + message);
					broadcastMessage(message);
					Thread.sleep(10);
				}
			} catch (Exception ex){
				System.out.println(ex);
			}
		}
	}
}
