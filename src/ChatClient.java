import java.net.*;
import java.nio.file.*;
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
	ObjectInputStream reader;
	ObjectOutputStream writer;
	
	boolean connected;
	boolean expectingFileName;
	boolean expectingFileData;
	
	String downloadFileName;
	
	public static void main (String[] args){
		try {
			ChatClient client = new ChatClient();
			client.setUpGUI();
		} catch (Exception e){
			System.out.println(e);
		}	
	}
	
	public ChatClient(){
		connected = false;
		expectingFileName = false;
		expectingFileData = false;
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
			reader = new ObjectInputStream(sock.getInputStream());
			writer = new ObjectOutputStream(sock.getOutputStream());
			writer.writeObject("CONNECT |" + usernameField.getText());
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
			writer.writeObject("DISCONNECT |" + usernameField.getText());
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
	
	void shareFile(String filePath){
		chatTextArea.append("< Sharing file " + filePath + ". >\n");
		try {
			writer.writeObject("SHARE_FILE |" + filePath);
			Path path = Paths.get(filePath);
			byte[] fileData = Files.readAllBytes(path);
			Thread.sleep(100);
			writer.writeObject(fileData);
		} catch (Exception ex) {
			chatTextArea.append("< Sharing file failed. >\n");
			ex.printStackTrace();
		}
	}
	
	void saveFile(byte[] fileData){
		try {
			Path file = Paths.get(downloadFileName);
			Files.write(file, fileData);
			chatTextArea.append("< Successfully downloaded file " + downloadFileName + "! >\n");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	// used by new thread to constantly check for new messages from server
	public class IncomingReader implements Runnable{
		public void run(){
			Object message;
			try {
				while (connected && (message = reader.readObject()) != null){
					if (expectingFileName){
						downloadFileName = (String)message;
						if (downloadFileName.equals("")){
							chatTextArea.append("< No file available to accept. >\n");
							expectingFileName = false;
							expectingFileData =  false;
						} else {
							chatTextArea.append("< Accepting file " + downloadFileName + ". >\n");
							expectingFileName = false;
							expectingFileData = true;
						}
					} else if (expectingFileData){
						chatTextArea.append("< Beginning download of file " + downloadFileName + ". >\n");
						saveFile((byte[])message);
						expectingFileData = false;
					} else {
						chatTextArea.append((String)message + "\n");
					}
					Thread.sleep(10);
				}
			} catch (Exception ex) {
				//ex.printStackTrace();
			}
		}
	}
	
	// handles regular chat messages as well as file sharing (/share) and downloading (/accept) commands
	public class SendButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e){
			try {
				String chatMessage = outgoingMsgField.getText().trim();
				if (!connected){
					chatTextArea.append("< Please connect to the server in order to send chat messages! >\n");
				} else if (!chatMessage.equals("")) {
					String[] splitMessage = chatMessage.split("\\s++");
					if (splitMessage[0].equals("/share")){
						if (splitMessage.length != 2){
							chatTextArea.append("< Invalid command. Send files using \"/share filePath\" >\n");
						} else {
							shareFile(splitMessage[1]);
						}
					} else if (splitMessage[0].equals("/accept")){
						expectingFileName = true;
						writer.writeObject("DL_FILE |" + usernameField.getText());
						writer.flush();						
					} else {
						writer.writeObject("CHAT |" + chatMessage);
						writer.flush();
					}
				}
			} catch (Exception ex){
				chatTextArea.append("< Sending message failed. >\n");
				ex.printStackTrace();
			}
			outgoingMsgField.setText("");
			outgoingMsgField.requestFocus();
		}
	}
	
	// connect/disconnect button. prompts user for server ip and port
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