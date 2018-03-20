import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ChatClient {
	
	JFrame frame;
	JLabel nameLabel;
	JTextField usernameField;
	JTextField outgoingMsgField;
	JTextArea chatTextArea;
	
	Socket sock;
	InputStreamReader stream;
	BufferedReader reader;
	PrintWriter writer;
	
	boolean connected;
	
	public static void main (String[] args){
				
		try {
			ChatClient client = new ChatClient();
			client.setUpGUI();
		} catch (Exception e){
			System.out.println(e);
		}	
	}
	
	void setUpGUI(){
		frame = new JFrame("Chat Client");
		JPanel mainPanel = new JPanel();
		chatTextArea = new JTextArea(20, 50);
		chatTextArea.setLineWrap(true);
		chatTextArea.setWrapStyleWord(true);
		chatTextArea.setEditable(false);
		JScrollPane qScroller = new JScrollPane(chatTextArea);
		qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		nameLabel = new JLabel("Username:");
		usernameField = new JTextField(20);
		usernameField.setText(randomUsername());
		outgoingMsgField = new JTextField(20);
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new SendButtonListener());
		JButton connectButton = new JButton("Connect");
		connectButton.addActionListener(new ConnectButtonListener());
		mainPanel.add(nameLabel);
		mainPanel.add(usernameField);
		mainPanel.add(connectButton);
		mainPanel.add(qScroller);
		mainPanel.add(outgoingMsgField);
		mainPanel.add(sendButton);
		frame.getContentPane().add(BorderLayout.CENTER,  mainPanel);
		frame.setSize(600,  440);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	String randomUsername(){
		Random rand = new Random();
		String name = "user" + (rand.nextInt(899999) + 100000); // 'user' + random 6 digit number
		return name;
	}
	
	// set up socket along with input and output
	void openConnection(String serverIP, int portNo){ // use port number > 1023
		try {
			sock = new Socket(serverIP, portNo);
			stream = new InputStreamReader(sock.getInputStream());
			reader = new BufferedReader(stream);
			writer = new PrintWriter(sock.getOutputStream());
			writer.println("CMD_CONNECT |" + usernameField.getText());
			writer.flush();
			connected = true;
		} catch (IOException e){
			chatTextArea.append("< Connection failed. >\n");
			System.out.println(e);
		}
	}
	
	void closeConnection(){
		try {
			connected = false;
			writer.print("CMD_DISCONNECT |" + usernameField.getText());
			writer.flush();
			writer.close();
			chatTextArea.append(usernameField.getText() + " has left the server.\n");
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
	
	void startReaderThread(){
		Thread readerThread = new Thread(new IncomingReader());
		readerThread.start();
	}
	
	void removeUsernameWhitespace(){
		String name = usernameField.getText();
		name = name.replaceAll("\\s","").trim();
		usernameField.setText(name);
	}
	
	// used by new thread to constantly check for new messages from server
	public class IncomingReader implements Runnable{
		public void run(){
			String message;
			try {
				while (connected && (message = reader.readLine()) != null){
					chatTextArea.append(message + "\n");
					Thread.sleep(10);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public class SendButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e){
			try {
				if (!connected){
					chatTextArea.append("< Please connect to the server in order to send chat messages! >\n");
				} else {
					String chatMessage = outgoingMsgField.getText();
					writer.println("TRANS_CHAT |" + chatMessage);
					writer.flush();
				}
			} catch (Exception ex){
				chatTextArea.append("< Sending message failed. >\n");
				ex.printStackTrace();
			}
			outgoingMsgField.setText("");
			outgoingMsgField.requestFocus();
		}
	}
	
	public class ConnectButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e){
			removeUsernameWhitespace();
			if (usernameField.getText().equals("")){
				chatTextArea.append("< Please set a username in order to join the server. >\n");
			} else if (!connected){
				String serverIP = JOptionPane.showInputDialog(frame, "Enter server IP address:", "New Connection", JOptionPane.QUESTION_MESSAGE);
				String serverPort = JOptionPane.showInputDialog(frame, "Enter server port number:", "New Connection", JOptionPane.QUESTION_MESSAGE);
				if (serverIP.equals("")) serverIP = "127.0.0.1"; // use defaults if not specified
				if (serverPort.equals("")) serverPort = "6666";
				openConnection(serverIP, Integer.parseInt(serverPort));
				if (connected){
					usernameField.setEditable(false);
					startReaderThread();
					outgoingMsgField.setText("");
					outgoingMsgField.requestFocus();
				}
			} else {
				closeConnection();
				usernameField.setEditable(true);
			}
		}
	}
	
}