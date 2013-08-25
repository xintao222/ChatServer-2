package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CryptoServer {

	public static void main(String[] args) throws Exception {
		int port = 5001;
		final ServerSocket welcomeSocket = new ServerSocket(port);
		final HashMap<String, ServerThread> onlineUsers = new HashMap<String, ServerThread>();

		// Start Accepting Client
		while (true) {
			System.out.println("Welcome to the Server: IP " + welcomeSocket.getLocalSocketAddress() + " PORT " + welcomeSocket.getLocalPort());
			Socket connectionSocket = welcomeSocket.accept();
			new ServerThread(connectionSocket, onlineUsers);
		}
	}
}

class ServerThread extends Thread {

	boolean allowed_to_talk = false;
	Socket connectionSocket;
	HashMap<String, ServerThread> onlineUsers = new HashMap<String, ServerThread>();
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	ServerThread talkingTo;

	public ServerThread(Socket connectionSocket, HashMap<String, ServerThread> h) {
		this.connectionSocket = connectionSocket;
		this.onlineUsers = h;
		this.start();
	}

	public void sendMessage(String s, ServerThread sender) throws IOException {
		talkingTo = sender;
		outToClient.writeBytes(s + '\n');
	}

	@Override
	public void run() {

		String userName = null;
		String password = null;

		boolean loggedIn = false;

		System.out.println("A new Cleint with Ip " + connectionSocket.getInetAddress().getHostName().toString() + ", just connected to this Server");

		// Create input and output readers for this Socket.
		try {
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			outToClient = new DataOutputStream(connectionSocket.getOutputStream());

			while (true) {
				String command = inFromClient.readLine();
				System.out.println(command);

				if (command.equals("\\REGISTER")) {
					System.out.println("Sending Response");
					outToClient.writeBytes("login: \n");
					userName = inFromClient.readLine();
					outToClient.writeBytes("passwd: \n");
					password = inFromClient.readLine();

					if (userName != null) {
						// Check if this user is already registered
						if (onlineUsers.containsKey(userName)) {
							outToClient.writeBytes("You are already Logged in \n");
						} else {
							onlineUsers.put(userName, this);
							outToClient.writeBytes("SUCCESSFULL REGISTRATION\n");
							loggedIn = true;
						}
					}
				} else if (command.equals("\\LOGOUT")) {
					onlineUsers.remove(userName);
					outToClient.writeBytes("LOGOUTGOODBYE\n");
					break;
				} else if (command.equals("\\LIST")) {
					// Check if the user is logged in
					if (loggedIn) {
						System.out.println("Sending list to the server");
						// Send all the userNames to the client if it requested
						// their list
						Set<String> c = onlineUsers.keySet();
						Iterator<String> itr = c.iterator();
						while (itr.hasNext()) {
							String name = itr.next();
							if (name.equals(userName)) continue;
							outToClient.writeBytes(name + "\n");
						}
					} else {
						outToClient.writeBytes("User is not logged in" + "\n");
					}
				}  else if (command.length() > 4 && command.substring(0, 5).equals("\\CHAT")) {
					// Check if the user is logged in
					if (loggedIn) {
						// Get the Name of the client this user wants to talk to

						if (command.length() < 7) {
							outToClient.writeBytes("You did not specify who to chat with\n");
							continue;
						}

						String chatPartner = command.substring(6).trim();
						// Check if this user is online
						if (onlineUsers.containsKey(chatPartner)) {
							ServerThread chatPartnerThread = onlineUsers.get(chatPartner);
							chatPartnerThread.sendMessage("_" + userName + "_ wants to chat with you, do you accept? Please answer Yes or No\n", this);
						} else {
							outToClient.writeBytes("This user is not online\n");
						}
					} else {
						outToClient.writeBytes("You are not logged in \n");
					}
					// If the client contacted to chat with, accepts!
				} else if (command.equals("Yes")) {
					talkingTo.sendMessage("Lets Chat\n", this);
					talkingTo.allowed_to_talk = true;
					allowed_to_talk = true;
					for (String s = inFromClient.readLine(); allowed_to_talk && talkingTo.allowed_to_talk && !s.startsWith("."); s = inFromClient.readLine()) {
						talkingTo.sendMessage("_" + userName + "_: " + s + "\n", this);
					}
					talkingTo.allowed_to_talk = false;
					allowed_to_talk = false;
					// talkingTo.sendMessage("chat ended" + "\n", this);
				} else if (command.equals("No")) {
					talkingTo.allowed_to_talk = false;
					allowed_to_talk = false;
					outToClient.writeBytes("Declined Chat\n");
				} else if (allowed_to_talk) {
					// need to send this message before the loop
					// talkingTo.sendMessage("Chat started:", this);

					for (String s = inFromClient.readLine(); allowed_to_talk && talkingTo.allowed_to_talk && !s.startsWith("."); s = inFromClient.readLine()) {
						talkingTo.sendMessage("_" + userName + "_: " + s + "\n", this);
					}
					talkingTo.allowed_to_talk = false;
					allowed_to_talk = false;
					// talkingTo.sendMessage("chat ended" + "\n", this);
				} else {
					outToClient.writeBytes("Unknown Command" + "\n");
				}
			}
		} catch (Exception e) {
			System.out.println("Some Problem " + e.getMessage());
			e.printStackTrace();
		}
	}
}