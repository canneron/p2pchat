import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Node{
	// Nickname of node, room its chatting in
	private String cNick, cRoom;
	// Are all messages synced
	private boolean cSynced;
	// IP Address of node
	private InetAddress cIp;
	// Port of node, number of local messages
	private int cPort, messageNo;
	// Timestamp of node creation
	private long time;
	// Socket that node listens on
	private DatagramSocket ds;
	// Storage of messages, locally sent messages
	private ArrayList<Message> messages;
	private ArrayList<Message> localMessages;
	// Chatrooms on network
	private HashMap<String, Integer> chatrooms;
	// Users on the chat network, users in chatroom
	ArrayList<Node> clients = new ArrayList<Node>();
	ArrayList<Node> room = new ArrayList<Node>();
	// Shut program down
	boolean quit;
	
	// Two constructors, first joins default chatroom, second joins a specified chatroom
	public Node(InetAddress ip, String nickname) {
		this.cNick = nickname;
		this.cIp = ip;
		this.cSynced = false;
		try {
			this.ds = new DatagramSocket(0);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.cPort = ds.getLocalPort();
		this.quit = false;
		this.setTime(System.currentTimeMillis() / 1000L);
		this.messages = new ArrayList<Message>();
		this.localMessages = new ArrayList<Message>();
		this.chatrooms = new HashMap<String, Integer>();
		this.setcRoom("default");
		this.setMessageNo(0);
	}
	
	public Node(InetAddress ip, String nickname, String room) {
		this.cNick = nickname;
		this.cIp = ip;
		this.cSynced = false;
		try {
			this.ds = new DatagramSocket(0);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.cPort = ds.getLocalPort();
		this.setTime(System.currentTimeMillis() / 1000L);
		this.messages = new ArrayList<Message>();
		this.chatrooms = new HashMap<String, Integer>();
		this.setcRoom(room);
		this.setMessageNo(0);
	}
	
	// A thread that listens on port 5000 for broadcasts from nodes wanting to join the network
	public class Broadcast extends Thread {
	    private byte[] buf = new byte[1024];
	    Node node;
	    boolean newNode = true;
	    
	    public Broadcast (Node lNode) {
	    	this.node = lNode;
	    }
	    
	    public void run() {
	        try {
	        	DatagramSocket socket = new DatagramSocket(5000);
		        while (!this.node.quit) {
		        	System.out.println("Server waiting for broadcast");
		            DatagramPacket packet = new DatagramPacket(buf, buf.length);
		            socket.receive(packet);
		            String data = ByteToString(buf);
		            // Split incoming message with '@' symbol and parse
		            String[] details = data.split("@");
		            // Nickname of node wanting to join
		            String newNN = details[1];
		            // Timestamp of new node
		            long timeS = Long.parseLong(details[2]);
		            // Chatroom of node wanting to join
		            String nodeChatRoom = details[3];
	            	if (newNN.equals(this.node.getNickname())) {
	            		// Skip if trying to add itself
	            		continue;
	            	} else {
	            		for (Node n: clients) {
	            			// If already in node list, skip
		            		if (n.getcNick().equals(newNN) && n.getTime() == timeS && n.getcIp() == packet.getAddress() && n.getcPort() == packet.getPort())
		            			newNode = false;
		            	}
	            		if (newNode) {
//	            			for (Node n: clients) {
//	            				// Skip if adding itself
//			            		if (n.getcIp() == this.node.getcIp() && n.getcPort() == this.node.getcPort() && n.getTime() == this.node.getTime()) {
//			            			continue;
//			            		} else {
//			            			// Send details of new node onto other nodes with /ADD tag
//			            			String aPkt = "/ADD@" + packet.getAddress() + "@" + packet.getPort() + "@" + newNN + "@" + timeS + "@" + nodeChatRoom;
//					            	DatagramPacket dp = new DatagramPacket(aPkt.getBytes(), aPkt.length(), n.getcIp(), n.getcPort());
//								    ds.send(dp);
//			            		}
//			            	}
		            		Node newClient = new Node(packet.getAddress(), newNN, nodeChatRoom);
			            	newClient.setcPort(packet.getPort());
			            	newClient.setTime(timeS);
			            	// Add to overall client list
			            	clients.add(newClient);
			            	// Send the new node this node's details
			            	String dPkt = "/INFO@" + this.node.getNickname() + "@" + this.node.getTime() + "@" + this.node.getcRoom();
			            	DatagramPacket dp = new DatagramPacket(dPkt.getBytes(), dPkt.length(), packet.getAddress(), packet.getPort());
						    ds.send(dp);
						    // Check if the new node is in the chat room
			            	if (this.node.getcRoom().equals(nodeChatRoom)) {
		            			System.out.println(this.node.getNickname() + " has joined the room");
		            			// Add to room list
		            			System.out.println("Before broadcast add: " + room.size());
			            		room.add(newClient);
			            		System.out.println("After broadcast add: " + room.size());
			            		// Increase population of chatroom
			            		int currVal = chatrooms.get(nodeChatRoom);
	            				chatrooms.put(nodeChatRoom, currVal + 1);
	            				// Send all messages in history as one string with characters to help parse later
						    	String msgsToSync = "";
				        		for (Message m: messages) {
				        			msgsToSync += m.getmSender() + ">" + m.getmMessage() + ">" + m.getmTStamp() + ">" +m.getMessageID() + "~";
				        		}
				        		// Send message history to new node
				        		String msgPkt = "/MSG@" + msgsToSync;
				        		System.out.println("Synced: " + msgPkt);
				            	DatagramPacket msgDp = new DatagramPacket(msgPkt.getBytes(), msgPkt.length(), packet.getAddress(), packet.getPort());
							    ds.send(msgDp);
		            		} else if (chatrooms.containsKey(nodeChatRoom)) {
		            			// If not in the same room, just increase value of room population
	            				int currentVal = chatrooms.get(nodeChatRoom);
	            				chatrooms.put(nodeChatRoom, currentVal + 1);
	            			} else {
	            				// If a new chatroom, add it to hash map
	            				chatrooms.put(nodeChatRoom, 1);
	            			}
	            		} else {
	            			// Reset
	            			newNode = true;
	            		}
	            	}
	            	
	            	buf = new byte[1024];
		        }
		        socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				quit = true;
				System.exit(1);
			}
	    }
	}
	
	public class Listener extends Thread {
		Node node;
	    
	    public Listener (Node lNode) {
	    	this.node = lNode;
	    }
	    
		public void run() {
			// Create a datagram socket on the port
			try {
				// Send out an initial packet to broadcast address for existing nodes to find
				chatrooms.put(this.node.getcRoom(), 1);
				ds.setBroadcast(true);
				// 255.255.255.255 is the designated broadcast address
				InetAddress bAdd = InetAddress.getByName("255.255.255.255");
				// Send out information
				String tStamp = "/INFO@" + this.node.getNickname() + "@" + this.node.getTime() + "@" + this.node.getcRoom();
				System.out.println(tStamp);
			    DatagramPacket dp = new DatagramPacket(tStamp.getBytes(), tStamp.length(), bAdd, 5000);
			    ds.send(dp);
				// create a byte array of size 65535 to receive data
		        byte[] receive = new byte[65535];
		  
		        // packet for receiving data
		        DatagramPacket packet = null;
		        
		        while (!this.node.quit) // keep going until quit
		        {
		        	System.out.println("CLIENT waiting for data");
		        	
		            // create the packet to receive data
		            packet = new DatagramPacket(receive, receive.length);
		         // receive the data into the packet
		            ds.receive(packet);
		            String data = ByteToString(receive);
		            String[] details = data.split("@");
		           for (String sr: details) {
		        	   if (sr.equals("localhost/127.0.0.1")) {
		        		   sr = "127.0.0.1";
           				}
		           }
		           // Initial exchange of details between nodes
		            if (details[0].equals("/INFO")) {
		            	if (ds.getBroadcast()) {
		            		ds.setBroadcast(false);
		            	}
		            	System.out.println("RETURNING " + this.node.getNickname() + " RECEIVED " + data);
		            	// Parse incoming data to create a new node to add to list and increment chat room values
		            	String newNN = details[1];
			            long timeS = Long.parseLong(details[2]);
			            String chatRoom = details[3];
            			Node newClient = new Node(packet.getAddress(), newNN, chatRoom);
		            	newClient.setcPort(packet.getPort());
		            	newClient.setTime(timeS);
		            	if (chatRoom.equals(this.node.getcRoom())) {
		            		System.out.println("Before info add: " + room.size());
		            		room.add(newClient);
		            		System.out.println("After info add: " + room.size());
		            		int currentVal = chatrooms.get(this.node.getcRoom());
		    				chatrooms.put(this.node.getcRoom(), currentVal + 1);
		            	} else if (chatrooms.containsKey(chatRoom)) {
            				int currentVal = chatrooms.get(chatRoom);
            				chatrooms.put(chatRoom, currentVal + 1);
            			} else {
            				chatrooms.put(chatRoom, 1);
            			}
				        clients.add(newClient);
				    // Add new node that has been forwarded
//		            } else if (details[0].equals("/ADD")) {
//		            	System.out.println(this.node.getNickname() + " ADDING " + details[3]);
//		            	System.out.println(details[1]);
//		            	details[1] = details[1].substring(1);
//		            	System.out.println(details[1]);
//		            	InetAddress newINA = InetAddress.getByName(details[1]);
//		            	int newPort = Integer.parseInt(details[2]);
//		            	String newNN = details[3];
//		            	long newTS = Long.parseLong(details[4]);
//		            	String chatRoom = details[5];
//		            	boolean newN = true;
//		            	// Check it is not an existing node
//		            	for (Node n: clients) {
//		            		if (newINA == n.getcIp() && newPort == n.getcPort() && newTS == n.getTime()) {
//		            			newN = false;
//		            		}
//		            	}
//		            	if (newN) {
//		            		Node newClient = new Node(newINA, newNN, chatRoom);
//			            	newClient.setcPort(newPort);
//			            	newClient.setTime(newTS);
//			            	if (chatRoom.equals(this.node.getcRoom())) {
//			            		System.out.println("Before add add: " + room.size());
//			            		room.add(newClient);
//			            		System.out.println("ADD add: " + room.size());
//			            		int currentVal = chatrooms.get(this.node.getcRoom());
//			    				chatrooms.put(this.node.getcRoom(), currentVal + 1);
//			            	} else if (chatrooms.containsKey(chatRoom)) {
//	            				int currentVal = chatrooms.get(chatRoom);
//	            				chatrooms.put(chatRoom, currentVal + 1);
//	            			} else {
//	            				chatrooms.put(chatRoom, 1);
//	            			}
//			            	clients.add(newClient);
//		            	}
//		            // Ends client session
		            } else if (details[0].equals("/QUIT")) {
		            	for (Node quitter: clients) {
		            		if (details[1].equals(this.node.getNickname()) && Long.parseLong(details[2]) == this.node.getTime()) {
		            			System.exit(0);
		            		}
		            		if (details[1].equals(quitter.getNickname()) && quitter.getTime() == Long.parseLong(details[2]) && Integer.parseInt(details[3]) == quitter.getTime() && InetAddress.getByName(details[4]) == quitter.getcIp()) {
		            			clients.remove(quitter);
		            			if (room.contains(quitter)) {
		            				room.remove(quitter);
		            				System.out.println(details[1] + " has left room");
		            				int currentVal = chatrooms.get(this.node.getcRoom());
		            				chatrooms.put(this.node.getcRoom(), currentVal - 1);
		            			}
		            			System.out.println(details[1] + " has disconnected");
		            			break;
		            		}
		            	}
		            // Receive a file and download it
		            } else if (details[0].equals("/FILE")) {
		            	// Ensure node cannot send it to itself
		            	for (String s: details) {
		            		System.out.println(s);
		            	}
		            	if (details[3].equals(this.node.getNickname())) {
		            		break;
		            	} else {
		            		System.out.println("");
			            	System.out.println("Incoming file: " + details[1] + " from " + details[3]);
			            		try {
			            			// Create a new file and parse the incoming details into it
				            	      File dwnld = new File(details[1]);
				            	      if (dwnld.createNewFile()) {
				            	    	  FileWriter fileWrite = new FileWriter(details[1]);
				            	    	  String[] fileContents = details[2].split(">");
				            	    	  for (String s: fileContents) {
				            	    		  fileWrite.write(s);
				            	    		  fileWrite.write(System.getProperty( "line.separator" ));
				            	    	  }
				            	    	  fileWrite.close();
				            	          System.out.println("File downloaded: " + dwnld.getName());
				            	      } else {
				            	    	  FileWriter fileWrite = new FileWriter(details[1]);
				            	    	  String[] fileContents = details[2].split(">");
				            	    	  for (String s: fileContents) {
				            	    		  fileWrite.write(s);
				            	    		  fileWrite.write(System.getProperty( "line.separator" ));
				            	    	  }
				            	    	  fileWrite.close();
				            	          System.out.println("File downloaded: " + dwnld.getName());
				            	      }
			            	    } catch (IOException e) {
			            	    	System.out.println("An error occurred.");
			            	    	e.printStackTrace();
			            	    }
		            	}
		            // Node parses incoming chat histories and adds to their chat history
		            } else if (details[0].equals("/MSG")) {
		            	if (!this.node.isSynced()) {
		            		this.node.setSynced(true);
			            	System.out.println("Syncing...");
			            	if (details.length > 1) {
			            		// Chat details are sent in 1 packet and parsed eg. <message1>~<message2>~<message3>
			            		String[] syncMsgs = details[1].split("~");
				        		if (this.node.messages.size() > 0) {
				        			int count = 0;
				        			for (count = 0; count < this.node.messages.size(); count++) {
				        				// Each message that has been parsed is now further parsed into its components
				        				// Eg <sender>@<message>@<timestamp>@messagenumber becomes sender | message | time stamp | message number
					        			String[] syncDetails = syncMsgs[count].split(">");
					        			// Check that this message is already in the history, if not add it
					        			if (syncDetails[0].equals(this.node.messages.get(count).getmSender()) && syncDetails[1].equals(this.node.messages.get(count).getmMessage()) && Long.parseLong(syncDetails[2]) == this.node.messages.get(count).getmTStamp() && Integer.parseInt(syncDetails[3]) == this.node.messages.get(count).getMessageID()) {
					        				count++;
					        				continue;
					        			} else {
					        				// Create new message, add it to the list and print it out
					        				Message newMsg = new Message(syncDetails[0], syncDetails[1], Long.parseLong(syncDetails[2]), Integer.parseInt(syncDetails[3]));
						        			newMsg.setMessageID(Integer.parseInt(syncDetails[3]));
						        			newMsg.setSynced(true);
						        			//System.out.println(syncDetails[0] + "> " + syncDetails[1]);
						        			messages.add(count, newMsg);
						        			count++;
					        			}
					        		}
				        			for (int i = count; i < syncMsgs.length; i++) {
					        			String[] syncDetails = syncMsgs[i].split(">");
				        				Message newMsg = new Message(syncDetails[0], syncDetails[1], Long.parseLong(syncDetails[2]), Integer.parseInt(syncDetails[3]));
					        			newMsg.setMessageID(Integer.parseInt(syncDetails[3]));
					        			newMsg.setSynced(true);
					        			//System.out.println(syncDetails[0] + "> " + syncDetails[1]);
					        			messages.add(newMsg);
					        		}
				        		} else {
				        			// If list is empty just add all messages without checking if they exist
				        			for (int i = 0; i < syncMsgs.length; i++) {
					        			String[] syncDetails = syncMsgs[i].split(">");
					        			Message newMsg = new Message(syncDetails[0], syncDetails[1], Long.parseLong(syncDetails[2]), Integer.parseInt(syncDetails[3]));
					        			newMsg.setMessageID(Integer.parseInt(syncDetails[3]));
					        			newMsg.setSynced(true);
					        			//System.out.println(syncDetails[0] + "> " + syncDetails[1]);
					        			messages.add(newMsg);
					        		}
				        		}
				        		System.out.print("\033[H\033[2J");  
				        		System.out.flush();  
				        		for (Message m: messages) {
				        			System.out.println(m.getmSender() + "> " + m.getmMessage());
				        		}
			            	}
		            	}
		            // Handles requests by nodes to change their nickname
		            } else if (details[0].equals("/NICKNAME")) {
		            	// Set stored node's nickname
		            	for (Node n: clients) {
		            		if (n.getNickname().equals(details[2]) && n.getTime() == Long.parseLong(details[3])) {
		            			n.setNickname(details[1]);
		            		}
		            	}
		            	// Change the nickname for senders in message history
		            	for (Message m: messages) {
		            		if (m.getmSender().equals(details[2])) {
		            			m.setmSender(details[1]);
		            		}
		            	}
		            // Handles a node creating a new room
		            } else if (details[0].equals("/CREATEROOM")) {
		            	for (Node n: clients) {
		            		if (details[1].equals(n.getNickname()) && n.getTime() == Long.parseLong(details[2])) {
		            			/** If this node is the one creating a room then clear all existing messages and nodes stored in room
		            			 *  Put new chatroom in the hashmap and decrease the value of the one that it is leaving
		            			 *  Add itself into the new room list
		            			 */
		            			if (details[1].equals(this.node.getNickname()) && this.node.getTime() == Long.parseLong(details[2])) {
				            		room.clear();
				            		messages.clear();
				            		int currentVal = chatrooms.get(this.node.getcRoom());
	                				chatrooms.put(this.node.getcRoom(), currentVal - 1);
	    		            		chatrooms.put(details[3], 1);
	    		            		this.node.setcRoom(details[3]);
				            		System.out.println("Joined chatroom " + details[3]);
				            		room.add(this.node);
				            	} else {
				            		// Otherwise, if in the same room as the node, remove it from the list and edit the hashmap accordingly
				            		if (this.node.room.contains(n)) {
				            			System.out.println("In room");
				            			room.remove(n);
	    	            				n.setcRoom(details[3]);
	    	            				chatrooms.put(details[3], 1);
	    	            				chatrooms.put(details[4], (this.node.chatrooms.get(details[4]) - 1));
	    		            			System.out.println(details[1] + " has left the chatroom");
	    		            		// Otherwise just edit the hashmap
				            		} else {
			            				chatrooms.put(details[3], 1);
			            				int val2 = chatrooms.get(details[4]);
			            				chatrooms.put(details[4], val2 - 1);
		            					n.setcRoom(details[3]);
				            		}
				            	}
    		            	}
            			}
		            // Handles a node joining an existing room from another existing room
		            } else if (details[0].equals("/JOINROOM")) {
		            	ArrayList<Node> tempList = new ArrayList<Node>();
		            	for (Node n: clients) {
		            		// Store the list of nodes in the room the node is joining
		            		if (n.getcRoom().equals(details[3])) {
		            			tempList.add(n);
		            		}
		            		if (details[1].equals(n.getNickname()) && n.getTime() == Long.parseLong(details[2])) {
		            			if (details[1].equals(this.node.getNickname()) && this.node.getTime() == Long.parseLong(details[2])) {
		            				// If this is the node joining the room, remove itself from existing room and clear the messages
				            		room.clear();
				            		messages.clear();
				            		// Adjust hash map 
				            		int currentVal = chatrooms.get(this.node.getcRoom());
	                				chatrooms.put(this.node.getcRoom(), currentVal - 1);
	    		            		int curVal = chatrooms.get(details[3]);
	    		            		chatrooms.put(details[3], curVal + 1);
	    		            		this.node.setcRoom(details[3]);
				            		System.out.println("Joined chatroom");
				            	} else {
				            		if (this.node.room.contains(n)) {
				            			// Remove from the room if the joining node is leaving
				            			room.remove(n);
	    	            				n.setcRoom(details[3]);
	    	            				int val = chatrooms.get(details[3]);
	    	            				chatrooms.put(details[3], val + 1);
	    	            				chatrooms.put(details[4], (this.node.chatrooms.get(details[4]) - 1));
	    		            			System.out.println(details[1] + " has left the chatroom");
				            		} else {
				            			int val = chatrooms.get(details[3]);
			            				chatrooms.put(details[3], val + 1);
			            				int val2 = chatrooms.get(details[4]);
			            				chatrooms.put(details[4], val2 - 1);
		            					n.setcRoom(details[3]);
				            		}
				            	}
    		            	}
            			}
		            	// Check this is the node joining, if so then add all existing nodes in the room to their list of clients in the chatroom
		            	if (details[1].equals(this.node.getNickname()) && this.node.getTime() == Long.parseLong(details[2])) {
		            		for (Node n2: tempList) {
		            			room.add(n2);
		            		}
		            		// Send a packet to those clients informing them this node is joining
		            		for (Node n3: room) {
		            			String roomInfo = "/ROOM@" + this.node.getNickname() + "@" + this.node.getTime();
							    DatagramPacket rpi = new DatagramPacket(roomInfo.getBytes(), roomInfo.length(), n3.getcIp(), n3.getcPort());
							    ds.send(rpi);
		            		}
		            		// Add itself to the list
		            		room.add(this.node);
		            	}
		            // Handles information of nodes joining the chat room a node is in
		            } else if (details[0].equals("/ROOM")) {
		            	for (Node n: clients) {
		            		if (details[1].equals(n.getNickname()) && n.getTime() == Long.parseLong(details[2])) {
	            				room.add(n);
	            				String msgsToSync = "";
				        		for (Message m: messages) {
				        			msgsToSync += m.getmMessage() + ">" + m.getmSender() + ">" + m.getmTStamp() + ">" +m.getMessageID() + "~";
				        		}
				        		// Send joining node the room's chat history
				        		String msgPkt = "/MSG@" + msgsToSync;
				        		System.out.println("Synced: " + msgPkt);
				            	DatagramPacket msgDp = new DatagramPacket(msgPkt.getBytes(), msgPkt.length(), packet.getAddress(), packet.getPort());
							    ds.send(msgDp);
		            			break;
		            		}
		            	}
		            // Handles requests for chat history/message syncing
		            } else if (details[0].equals("/MSGREQ")) {
		            	if (details[1].equals(this.node.getNickname()) && this.node.getTime() == Long.parseLong(details[2])) {
		            		continue;
		            	} else {
		            		// A node will send a request to all other nodes in the room for their chat histories, and when this is received they will send them back
		            		for (Node c: room) {
		            			if (c.getNickname().equals(details[1]) && c.getTime() == Long.parseLong(details[2])) {
		            				String msgsToSync = "";
					        		for (Message m: messages) {
					        			msgsToSync += m.getmMessage() + ">" + m.getmSender() + ">" + m.getmTStamp() + ">" +m.getMessageID() + "~";
					        		}
					        		String msgPkt = "/MSG@" + msgsToSync;
					        		System.out.println("Synced: " + msgPkt);
					            	DatagramPacket msgDp = new DatagramPacket(msgPkt.getBytes(), msgPkt.length(), c.getcIp(), c.getcPort());
								    ds.send(msgDp);
		            			}
		            		}
		            	}
		            // Allows a node to find missing data
		            } else if (details[0].equals("/SYNCREQ")) {
		            	// If the amount of messages sent by a node stored by the requester is the same as the number the node has sent
		            	if (Integer.parseInt(details[1]) == this.node.messageNo) {
		            		System.out.println("Synced");
		            		// If so reply that the node is synced
		            		String syncReply = "/SYNCREPLY@" + this.node.getNickname() + "@SYNCED";
		            		DatagramPacket drp = new DatagramPacket(syncReply.getBytes(), syncReply.length(), packet.getAddress(), packet.getPort());
						    ds.send(drp);
		            	} else {
		            		System.out.println("Not Synced");
		            		// Otherwise send a list of its local chat history (messages sent by this node) back to the requester
		            		String localMsgs = "/SYNCREPLY@" + this.node.getNickname() + "@";
		            		for (Message msg: this.node.localMessages) {
		            			localMsgs += msg.getmSender() + ">" + msg.getmMessage() + ">" + msg.getmTStamp() + ">" + msg.getMessageID() + "~";
		            		}
		            		DatagramPacket drp = new DatagramPacket(localMsgs.getBytes(), localMsgs.length(), packet.getAddress(), packet.getPort());
						    ds.send(drp);
		            	}
		            // Reply to the request for missing data
		            } else if (details[0].equals("/SYNCREPLY")) {
		            	ArrayList<String> missingMsgs = new ArrayList<String>();
		            	ArrayList<Message> senderMsgs = new ArrayList<Message>();
		            	System.out.println("Reply received");
		            	if (details[2].equals("SYNCED")) {
		            		System.out.println(details[1] + ": Messages Synced");
		            	} else {
		            		// If missing messages parse messages similarly to how they parsed in /MSG when adding chat history
		            		System.out.println(details[1] + ": Missing Messages");
		            		System.out.println("Syncing...");
		            		String[] nodeMsgs = details[2].split("~");
		            		// Create a list of messages sent by the sender stored on this node
		            		for (Message m: this.node.messages) {
	            				if (m.getmSender().equals(details[1])) {
	            					senderMsgs.add(m);
	            				}
	            			}
		            		// Check each missing message
		            		for (String s: nodeMsgs) {
		            			boolean missing = true;
		            			String[] msgDetails = s.split(">");
		            			// If this message is not found in the existing list then add it to the missing messages list
		            			for (Message m: senderMsgs) {
		            				if (m.getMessageID() == Integer.parseInt(msgDetails[3])) {
		            					missing = false;
		            				}
		            			}
		            			if (missing) {
		            				missingMsgs.add(s);
		            			}
		            		}
		            		for (String mm: missingMsgs) {
		            			System.out.println(mm);
		            		}
		            		boolean inserted = false;
		            		for (String mm: missingMsgs) {
		            			String[] mmDetails = mm.split(">");
		            			// Now insert the missing messages back into the chat history at their correct points based on their time stamp
	            				for (int l = 0; l < this.node.messages.size(); l++) {
	            					if (Long.parseLong(mmDetails[2]) < messages.get(l).getmTStamp()) {
	            						if (inserted) {
	            							continue;
	            						} else {
	            							messages.add(l, new Message(mmDetails[1], mmDetails[0], Long.parseLong(mmDetails[2]), Integer.parseInt(mmDetails[3])));
		            						inserted = true;
	            						}
	            					}
	            				}
	            				if (!inserted) {
	            					messages.add(new Message(mmDetails[1], mmDetails[0], Long.parseLong(mmDetails[2]), Integer.parseInt(mmDetails[3])));
	            				}
	            			}
		            		System.out.println("Synced");
		            	}
		            // Handle normal chat messages
		            } else {
		            	int mSize = messages.size();
		            	for (int i = 0; i < mSize; i++) {
		            		// If existing message has been sent out of order put it back in place
		            		if (Long.parseLong(details[2]) < messages.get(i).getmTStamp()) {
		            			messages.add(i, new Message(details[0], details[1], Long.parseLong(details[2]), Integer.parseInt(details[3])));
		            		} else {
		            			if (i == mSize - 1) {
		            				messages.add(new Message(details[0], details[1], Long.parseLong(details[2]), Integer.parseInt(details[3])));
		            			} else {
		            				continue;
		            			}
		            		}
		            	}
		            	if (mSize == 0) {
		            		messages.add(new Message(details[0], details[1], Long.parseLong(details[2]), Integer.parseInt(details[3])));
		            	}
		            	// Print out the incoming message as: username> message
		            	System.out.println(details[0] + "> " + details[1]);
		            }
		            	
		            // Create new empty buffer after each packet
		            receive = new byte[65535];
		        }
		        ds.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				quit = true;
				System.exit(1);
			}
		}
	}
	
	// Thread for sending messages to other nodes on the network
	public class Sender extends Thread {
		Node node;
	    
	    public Sender (Node lNode) {
	    	this.node = lNode;
	    }
		public void run() {
			Scanner sc = new Scanner(System.in);
			  
	        // Create a datagram socket seperate to the existing listening one
	        DatagramSocket sendDs;
	        ;
	        long msgTS = 0;
	        String message = "";
			try {
			    byte buf[] = null;
			    sendDs = new DatagramSocket();
			    // Designations for type of messages
			    // Text messages are sent and parsed normally as chat messages between users
			    boolean textMsg = false;
			    // Command messages are not sent and dealt with locally
			    boolean userCommand = false;
			    // Simulates a failure in the network
			    boolean failuresim = false;
			    int failNode = 0;
			    System.out.println("Type '/list' to see commands");
		        while (!this.node.quit) // loop until a quit condition
		        {
		        	// read the input from the user
		            String input = sc.nextLine();
		            // Time stamp the message
		            msgTS = System.currentTimeMillis();
		            // Quits program
		            if (input.equalsIgnoreCase("/quit")) {
		            	System.out.println("Shutting Down");
		            	message = "/QUIT@" + this.node.getNickname() + "@" + this.node.getTime() + "@" + this.node.getcPort() + "@" + this.node.getcIp(); 
		                this.node.quit=true;
		            // Prints a list of commands
		            } else if (input.equalsIgnoreCase("/list")) {
		            	System.out.println("/quit		Leave the chat");
		            	System.out.println("/nickname	Set a new username");
		            	System.out.println("/robot		Starts automated chat bot to generate dialogue");
		            	System.out.println("/joinroom	Join a new chatroom");
		            	System.out.println("/createroom	Create a new chatroom");
		            	System.out.println("/viewrooms	View a list of existing chatrooms");
		            	System.out.println("/sendfile	Send a file to other nodes in your chatroom");
		            	System.out.println("/redraw		Reprint the chat in order");
		            	System.out.println("/rebuilddata	Clear local data and rebuild from network");
		            	System.out.println("/syncdata	Check for missing data");
		            	userCommand = true;
		            // Allows user to set a nickname
		            } else if (input.equalsIgnoreCase("/nickname")) {
		            	System.out.println("");
		            	System.out.print("Enter Nickname: ");
		            	String input2 = sc.nextLine();
		            	int nicknameCount = 0;
		            	for (Node c: clients) {
		            		if (c.getNickname().equals(input2)) {
		            			nicknameCount += 1;
		            		}
						}
		            	if (nicknameCount > 0) {
		            		message = "/NICKNAME@" + input2 + "(" + nicknameCount + ")@" + this.node.getNickname() + "@"+ this.node.getTime();
		            		this.node.setNickname(input2 + "(" + nicknameCount + ")");
		            	} else {
		            		message = "/NICKNAME@" + input2 + "@" + this.node.getNickname() + "@" + this.node.getTime();
		            		this.node.setNickname(input2);
		            	}
		            // Allows the user to view chat rooms on the network and who is in their current room
		            } else if (input.equalsIgnoreCase("/viewrooms")) {
		            	System.out.println("-------------------------------------");
		            	System.out.println("+ Current Room: ");
						System.out.println(this.node.getcRoom());
		            	System.out.println("+ Users in room: "); 
		            	for (Node inRoom: room) {
		            		System.out.println(inRoom.getNickname());
		            	}
		            	System.out.println("CHATROOM	# OF USERS");
		            	System.out.println("-------------------------------------");
		            	for (Map.Entry<String, Integer> set : chatrooms.entrySet()) {
		            		System.out.println(set.getKey() + "		" + set.getValue());
		            	}
		            	userCommand = true;
		            // Check who is in the current chat room
		            } else if (input.equalsIgnoreCase("/checkroom")) {
		            	System.out.println("In room:");
		            	for (Node k: room) {
		            		System.out.println(k);
		            	}
						userCommand = true;
					// Creates a new chatroom
		            } else if (input.equalsIgnoreCase("/createroom")) {
		            	System.out.println("");
		            	System.out.print("Enter Room Name: ");
		            	String input2 = sc.nextLine();
		            	int roomCount = 0;
		            	// If the name of the room exists add a number to the end to distinguish it
		            	for (Map.Entry<String, Integer> set : chatrooms.entrySet()) {
		            		if (set.getKey().equals(input2)) {
		            			roomCount++;
		            		}
		            	}
		            	String roomName;
		            	if (roomCount > 0) {
		            		roomName = input2 + "(" + roomCount + ")";
		            	} else {
		            		roomName = input2;
		            	}
		            	System.out.println("Disconnected from " + this.node.getcRoom());
		            	// Send notice to other nodes on the network
	            		message = "/CREATEROOM@" + this.node.getNickname() + "@" + this.node.getTime() + "@" + roomName + "@" + this.node.getcRoom();
	            	// Allows a user to join an existing room
		            } else if (input.equalsIgnoreCase("/joinroom")) {
		            	System.out.println("");
		            	System.out.print("Enter Room Name: ");
		            	String input2 = sc.nextLine();
		            	// If room exists send notice to other nodes in the current chatroom, otherwise advise user it doesn't exist
		            	if (chatrooms.containsKey(input2)) {
		            		System.out.println("Disconnected from " + this.node.getcRoom());
		            		message = "/JOINROOM@" + this.node.getNickname() + "@" + this.node.getTime() + "@" + input2 + "@" + this.node.getcRoom();
		            	} else {
		            		System.out.println("Room does not exist");
		            		userCommand = true;
		            	}
		            // Redraw the chat history in order
		            } else if (input.equalsIgnoreCase("/redraw")) {
		            	this.node.redraw(this.node);
		            	userCommand = true;
		            // Send a request for other nodes chat histories to rebuild it
		            } else if (input.equalsIgnoreCase("/rebuilddata")) {
		            	message = "/MSGREQ@" + this.node.getNickname() + "@" + this.node.getTime();
		            	this.node.messages.clear();
		            // Send a text file across the network
		            }else if (input.equalsIgnoreCase("/sendfile")) {
		            	message += "/FILE@";
		            	System.out.println("");
		            	// Get file path
		            	System.out.println("");
		            	System.out.print("Enter full path of file: ");
		            	String input3 = sc.nextLine();
		            	message += input3 + "@";
		            	File f = new File(input3);
		            	// Check file exists
		            	if(f.exists() && !f.isDirectory()) { 
		            		// Reads in each line and adds it to the message to be sent with the '>' as a delimiter to allow parsing on the other end
		            		BufferedReader br = new BufferedReader(new FileReader(f));
		            		String data = br.readLine();
		            		while(data!=null)
		            		{
		            		    message += data + ">";  // Writing in the console
		            		    data = br.readLine();
		            		 }
		            		br.close();
		            	} else {
		            		System.out.println("Invalid file, try again");
		            	}
		            	message += "@" + this.node.cNick;
		            	System.out.println("Sent file");
		            	textMsg = true;
		            // Start chat bot
		            } else if (input.equalsIgnoreCase("/robot")) {
		        		long t= System.currentTimeMillis();
		        		long end = t+15000;
		        		while(System.currentTimeMillis() < end) {
		        			String[] greetings = {"Hello", "Hi", "Yo", "..."};
			            	Random r = new Random();
			        		int low = 1;
			        		int high = 4;
			        		System.out.println(this.node.getNickname() + "> " + greetings[(r.nextInt(high-low) + low) - 1]);
			        		Thread.sleep(3000);
			        		userCommand = true;
		        		}
		        	// Generate a random number of a node to be designated to not receive a message to simulate a failure
		            } else if (input.equalsIgnoreCase("/failsim")) {
		            	Random r = new Random();
		            	boolean notNode = true;
		            	while (notNode) {
		            		int low = 1;
			        		int high = room.size();
			        		failNode = (r.nextInt(high-low) + low) - 1;
			        		if (!room.get(failNode).getNickname().equals(this.node.getNickname())) {
			        			notNode = false;
			        		}
		            	}
		        		failuresim = true;
		        		message = node.getNickname() + "@" + "Failure test" + "@" + msgTS + "@" + this.node.getMessageNo();
		            	localMessages.add(this.node.messageNo, new Message("Failure Test", this.node.getNickname(), msgTS, this.node.getMessageNo()));
		            	this.node.setMessageNo(this.node.getMessageNo() + 1);
		            // Send a request to sync data by counting the amount of messages each node has sent that is stored on the local chat history
		            // Sends it to each node to confirm the number is right
		            } else if (input.equalsIgnoreCase("/syncdata")) {
		        		for (Node c: room) {
		        			int count = 0;
		        			for (int i = 0; i < this.node.messages.size(); i++) {
		        				if (c.getNickname().equals(this.node.messages.get(i).getmSender())) {
		        					count++;
		        				}
		        			}
		        			if (count > 0) {
		        				String missingMsg = "/SYNCREQ@" + count;
							    DatagramPacket drp = new DatagramPacket(missingMsg.getBytes(), missingMsg.length(), c.getcIp(), c.getcPort());
							    ds.send(drp);
		        			}
		        		}
		        		userCommand = true;
		        	// Reads in and parses a web page and outputs the title of the web page
		            } else if (input.equalsIgnoreCase("/readtitle")) {
		                try {
		                	System.out.println("");
			            	System.out.print("Enter URL: ");
			            	String urlin = sc.nextLine();
		                    URL url = new URL(urlin);
		                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		                    String addResult;
		                    String result = "";
		                    while ((addResult = in.readLine()) != null) {
		                        result += addResult;
		                    }
		                    result = result.substring(result.indexOf("<title") + 7);
		                    result = result.substring(result.indexOf(">") + 1);
		                    result = result.substring(0, result.indexOf("</title>"));
		                    System.out.println("Webpage title: ");
		                    System.out.println(result);
		                } catch (MalformedURLException ex) {
		                	System.out.println("Enter valid url");
		                } catch (IOException ex) {
		                    ex.printStackTrace();
		                }
		                userCommand = true;
		            // If just sending a message put together a message and add to local messages
		            } else {
		            	message = node.getNickname() + "@" + input + "@" + msgTS + "@" + this.node.getMessageNo();
		            	localMessages.add(this.node.messageNo, new Message(input, this.node.getNickname(), msgTS, this.node.getMessageNo()));
		            	this.node.setMessageNo(this.node.getMessageNo() + 1);
		            	textMsg = true;
		            }
		            
		            // convert the string into a byte array
		            buf = message.getBytes();
		            // Cases that determine how the message is sent and who it is sent to
		            if (textMsg) {
		            	for (Node c: room) {
						    DatagramPacket dp = new DatagramPacket(buf, buf.length, c.getcIp(), c.getcPort());
						    sendDs.send(dp); 
						}
		            } else if (userCommand) {
		            	continue;
		            }  else if (failuresim) {
		            	for (int i = 0; i < room.size(); i++) {
		            		if (i == failNode) {
		            			continue;
		            		} else {
		            			DatagramPacket dp = new DatagramPacket(buf, buf.length, room.get(i).getcIp(), room.get(i).getcPort());
							    sendDs.send(dp); 
		            		}
						}
		            } else {
	            		for (Node c: clients) {
						    DatagramPacket dp = new DatagramPacket(buf, buf.length, c.getcIp(), c.getcPort());
						    sendDs.send(dp); 
						}
		            }
		            
		            textMsg = false;
		            userCommand = false;
		        }
		        sendDs.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				quit = true;
				System.exit(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        sc.close();
		}
	}
	
	// Called in StartNode to start the node
	public void Start(Node localNode) throws UnknownHostException, SocketException {
		System.out.println(localNode.getNickname() + " has connected");
		// Adds itself to the room and client list
		clients.add(localNode);
		room.add(localNode);
		Broadcast broadcaster = new Broadcast(localNode);
		Sender send = new Sender(localNode);
		Listener listen = new Listener(localNode);
		// Listener is started first so that it can send the broadcast packet which is then picked up by any existing nodes on the network
		listen.start();
		// Broadcaster thread starts to listen to any new nodes
		broadcaster.start();
		// Finally start thread to send messages
		send.start();
	}
	
	// If every message in the node's chat history is synced set the node to synced
	public void checkSync(Node lNode) {
		boolean sync = true;
		for (Message m: lNode.messages) {
			if (!m.isSynced()) {
				sync = false;
			}
		}
		if (sync) {
			lNode.setSynced(true);
		} else {
			lNode.setSynced(false);
		}
	}
	
	// Print chat history in order
	public void redraw(Node lnode) {
		System.out.print("\033[H\033[2J");  
		System.out.flush();  
		for (int i = 0; i < lnode.messages.size(); i++) {
			System.out.println(lnode.messages.get(i).getmSender() + "> " + lnode.messages.get(i).getmMessage());
		}
		System.out.println("Chat Refreshed");
	}
	
    // Convert byte array into string
    public static String ByteToString(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret.toString();
    }
    
	// Setters and getters
	public String getcNick() {
		return cNick;
	}

	public void setcNick(String cNick) {
		this.cNick = cNick;
	}
	
	public InetAddress getcIp() {
		return cIp;
	}

	public void setcIp(InetAddress cIp) {
		this.cIp = cIp;
	}

	public void setNickname(String nn) {
		this.cNick = nn;
	}
	
	public String getNickname() {
		return cNick;
	}

	public int getcPort() {
		return cPort;
	}

	public void setcPort(int cPort) {
		this.cPort = cPort;
		ds.close();
	}
	
	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public boolean isSynced() {
		return cSynced;
	}

	public void setSynced(boolean cSynced) {
		this.cSynced = cSynced;
	}

	public String getcRoom() {
		return cRoom;
	}

	public void setcRoom(String cRoom) {
		this.cRoom = cRoom;
	}

	public int getMessageNo() {
		return messageNo;
	}

	public void setMessageNo(int messageNo) {
		this.messageNo = messageNo;
	}
}
