import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ChatClient {
	
	JLabel nameLabel;
	JTextField username;
	JTextField outgoing;
	JTextArea incoming;
	
	Socket sock;
	InputStreamReader stream;
	BufferedReader reader;
	PrintWriter writer;
	
	public static void main (String[] args){
		try {
			ChatClient client = new ChatClient();
			client.go();
		} catch (Exception e){
			System.out.println(e);
		}	
	}
	
	void go(){
		setUpGUI();
		openConnection();
		startReaderThread();
	}
	
	void setUpGUI(){
		JFrame frame = new JFrame("Chat Client");
		JPanel mainPanel = new JPanel();
		incoming = new JTextArea(20, 50);
		incoming.setLineWrap(true);
		incoming.setWrapStyleWord(true);
		incoming.setEditable(false);
		JScrollPane qScroller = new JScrollPane(incoming);
		qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		nameLabel = new JLabel("Username:");
		username = new JTextField(20);
		outgoing = new JTextField(20);
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new SendButtonListener());
		mainPanel.add(nameLabel);
		mainPanel.add(username);
		mainPanel.add(qScroller);
		mainPanel.add(outgoing);
		mainPanel.add(sendButton);
		frame.getContentPane().add(BorderLayout.CENTER,  mainPanel);
		frame.setSize(600,  440);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	void openConnection(){ // default for testing on local host
		openConnection("127.0.0.1", 6666);
	}
	
	// set up socket along with input and output
	void openConnection(String machineName, int portNo){ // use port number > 1023
		try {
			sock = new Socket(machineName, portNo);
			stream = new InputStreamReader(sock.getInputStream());
			reader = new BufferedReader(stream);
			writer = new PrintWriter(sock.getOutputStream());
		} catch (IOException e){
			incoming.append("< Connection failed. >\n");
			System.out.println(e);
		}
	}
	
	void startReaderThread(){
		Thread readerThread = new Thread(new IncomingReader());
		readerThread.start();
	}
	
	void closeConnection(){
		try {
			reader.close();
			writer.close();
		} catch (IOException e){
			System.out.println(e);
		}
	}
	
	// used by new thread to constantly check for new messages from server
	public class IncomingReader implements Runnable{
		public void run(){
			String message;
			try {
				while ((message = reader.readLine()) != null){
					incoming.append(message + "\n");
				}
			} catch (Exception ex) {
				System.out.println(ex);
			}
		}
	}
	
	public class SendButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e){
			try {
				if (username.getText().equals("")){
					incoming.append("< Please set a username in order to send chat messages! >\n");
				} else {
					String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
					String chatMessage = timeStamp + " " + username.getText() + " : " + outgoing.getText();
					writer.println(chatMessage);
					writer.flush();
				}
			} catch (Exception ex){
				incoming.append("< Sending message failed. >\n");
				System.out.println(ex);
			}
			outgoing.setText("");
			outgoing.requestFocus();
		}
	}
	
}