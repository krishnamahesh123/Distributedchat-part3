package PeerToPeerChat; //import the package
 
//import all the libraries
import java.io.BufferedReader; 
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import the custom message class
import message.Message;

//class for defining a Peer Node
public class PeerNode {

	public static void main(String args[]) throws Exception{
		
		PeerNode serverComponent = new PeerNode();
		new Thread() {
            public void run() {
        
            	serverComponent.startServer();
            }
		}.start();
		
		startPeer();
		
	}
	
	//some variables for server and client components
	static ServerSocket listeningSocket;
	Socket acceptedClient;
	ExecutorService threadPool = null;
	int number_of_clients = 0;

	//constructor to initialize the member variables
	PeerNode(){
		
		//run a pool for 12 threads, so that at a same time, each node can have 12 connected peers
		//can modify the number as required
		threadPool = Executors.newFixedThreadPool(12);
		
	}
	
	//sockets, input streams, and output streams for client component of the peer 
	static ArrayList<Socket> clientConnectionSockets = new ArrayList<Socket>(); 
	static ArrayList<ObjectOutputStream> outputStreamArray = new ArrayList<ObjectOutputStream>(); 
	static ArrayList<ObjectInputStream> inputStreamArray = new ArrayList<ObjectInputStream>();
	
	public void listenForConnections() {
		//a forever loop to listen for incoming connections
		while(true) {
			
			try {
				acceptedClient = listeningSocket.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//accept a new client
			number_of_clients++;//increment clients counter
			clientConnectionSockets.add(acceptedClient);//add the client to the arraylist of all connected clients
			ServerThread runnable = null;
			try {
				runnable = new ServerThread(acceptedClient, number_of_clients, this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//run a separate thread to receive messages from new client			
			threadPool.execute(runnable);//execute the thread
			
		}		
		
	}
	
	//a function to start the server component of the peer node
	public void startServer(){
		
		//creates a socket to listen for incoming messages, and prints a message
		//also prints relevant details for another node to connect
		try {
			listeningSocket = new ServerSocket(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		try {
			System.out.println("Server IP: " + listeningSocket.getInetAddress().getLocalHost());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Server Port: " + listeningSocket.getLocalPort());		
		
		listenForConnections();
		
	}
	

	//sockets, input streams, and output streams for server component of the peer 
	static ArrayList<Socket> serverConnectionSockets = new ArrayList<Socket>(); 
	static ArrayList<ObjectOutputStream> sOutputArray = new ArrayList<ObjectOutputStream>(); 
	static ArrayList<ObjectInputStream> sInputArray = new ArrayList<ObjectInputStream>();
	static BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));
	
	public static String readCommand() throws IOException {
		
		//input command from user
		System.out.print("Enter Command: ");
		String cmd = systemIn.readLine();
		return cmd;
		
	}
	
	//server component for receiving messages from nodes and sending to all other connected nodes
	
	private static class ServerThread implements Runnable {
		
		PeerNode server = null;
		Socket client = null;
		ObjectInputStream cin;
		ObjectOutputStream cout;
		Scanner sc = new Scanner(System.in);
		int id;
		Message msg;
		
		ServerThread(Socket client, int count, PeerNode server) throws IOException{
			
			this.client = client;
			this.server = server;
			this.id = count;
			
			System.out.println("Connection " + id + " established with client " + client);
			
			cin = new ObjectInputStream(client.getInputStream());
			cout = new ObjectOutputStream(client.getOutputStream());
			outputStreamArray.add(cout);
			inputStreamArray.add(cin);
						
		}
		
		@Override 
		public void run() {
			
			int x = 1;
			
			try {
				
				while(true) {
					
					msg = (Message)cin.readObject();
					
					if (msg.getType() == Message.NOTE) {
						
						String content = (String) msg.getContent();
						System.out.println(content);						

						//cout.writeObject(content);
						
						for(int i = 0; i < outputStreamArray.size(); i++) {
							
							outputStreamArray.get(i).writeObject(content);
							
						}
						
					}
					
					else if(msg.getType() == Message.LEAVE) {
						
						System.out.println("Closing socket for client: " + client);
						cin.close();
						cout.close();
						client.close();
						outputStreamArray.remove(cout);
						inputStreamArray.remove(cin);
						clientConnectionSockets.remove(client);
						return;
					}
					else if(msg.getType() == Message.SHUTDOWN) {
						
						System.out.println("Shutting socket for client: " + client);
						cin.close();
						cout.close();
						client.close();
						outputStreamArray.remove(cout);
						inputStreamArray.remove(cin);
						clientConnectionSockets.remove(client);
						return;

					}					
					else if(msg.getType() == Message.SHUTDOWN_ALL) {
						
						try {
							for(int i = 0; i < outputStreamArray.size(); i++) {
								
								outputStreamArray.get(i).writeObject((String) msg.getContent());
								
							}
						}
						catch(SocketException se) {
							
							//
							System.exit(0);
							
						}
						
						for(int i = 0; i < clientConnectionSockets.size(); i++) {
							
							outputStreamArray.get(i).close();
							inputStreamArray.get(i).close();
							clientConnectionSockets.get(i).close();
							
						}
						server.listeningSocket.close();
						
						System.exit(0);
						
					}
					else if(msg.getType() == Message.JOIN) {
						
						String s = "Connection Established!";
						cout.writeObject(s);
						
					}
					
				}
			
				
			} catch (IOException ignore) {
				
				//do nothing
				
			} catch (ClassNotFoundException ignore) {
				
				//do nothing
				
			}
			
		}
		
	}
	
	public static void joinChat(NodeInfo currentNode) throws UnknownHostException, IOException {
		
		//take input for the node to join
		String serverIp = "";
		int serverPort = 0;
	
		Scanner inputReader = new Scanner(System.in);  // Create a Scanner object for keyboard inputs		

		System.out.println("Enter IP to connect: ");
		serverIp = inputReader.nextLine();
				
		System.out.println("Enter Port to connect: ");		
		serverPort = Integer.parseInt(inputReader.nextLine());
		
		//open a new socket to join the node
		Socket sk = new Socket(serverIp, serverPort);
		
		//new input/output objects for the new node
		ObjectOutputStream sout = new ObjectOutputStream(sk.getOutputStream());
		ObjectInputStream sin = new ObjectInputStream(sk.getInputStream());		
		
		//add the socket, input/output objects to respective lists for keeping tracks
		serverConnectionSockets.add(sk);
		sOutputArray.add(sout);
		sInputArray.add(sin);				
		
		//create a new message object, and fill it with content
		Message msg = new Message(Message.JOIN, "JOIN");
		
		System.out.println("Joined chat at: " + sk);
		
		//create a new thread to receive message from the newly connected client
		Thread receiver = new Thread( new Runnable() {
			
			String msg = currentNode.getName() + ": ";
			
			@Override
			public void run() {
				boolean shutdown = false;
				
				try {
					
					String s = "";
					//String s = (String) sin.readObject();
					
					while(true && !shutdown) {
					
						try {
						
							
							try {
								s = (String) sin.readObject();
							}
							catch(EOFException e) {
								
								//
								
							}
							//check if a shutdown message is received, and set the shutdown flag to true
							if(s.contains("shutdown_all") || s.contains("SHUTDOWN_ALL")) {
								
								shutdown = true;
								return;
								
							}
						
							System.out.println(s);
							
						}
						catch (SocketException ignore) {
							
							//nothing
							
						}

						
					}
				
					
				}
				
				catch (IOException e) {
					
					e.printStackTrace();
					
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		});

		receiver.start();
		
	}
	
	public static void leaveChat(NodeInfo currentNode) throws IOException {
		
		Scanner inputReader = new Scanner(System.in);  // Create a Scanner object for keyboard inputs		

		//cannot leave if not connected to any node
		if(serverConnectionSockets.size() == 0) {
			
			System.out.println("You need to join a chat first!");
			return;
			
		}
		
		//print all connects sockets to ask where to leave
		for(int i = 0; i < serverConnectionSockets.size(); i++) {
			
			System.out.println((i+1));
			System.out.println("IP: " + serverConnectionSockets.get(i).getInetAddress().getLocalHost());
			System.out.println("Port: " + serverConnectionSockets.get(i).getLocalPort());
			System.out.println();
			
		}

		//take input of the node to leave
		System.out.println("Enter IP of node to leave: ");
		String ipToLeave = inputReader.nextLine();

		System.out.println("Enter IP of node to leave: ");
		int portToLeave = Integer.parseInt(inputReader.nextLine());;
		
		//create a new message object to send to the node to leave
		Message msg = new Message(Message.LEAVE, currentNode.getName() + ": " + "LEAVE");
		
		//receiver.stop();						
		
		//send message and remove the node from all the maintained. Close the connections first
		for(int i = 0; i < serverConnectionSockets.size(); i++) {
			
			if(serverConnectionSockets.get(i).getLocalAddress().getLocalHost().toString().contains(ipToLeave) && serverConnectionSockets.get(i).getPort() == portToLeave){
				
				sOutputArray.get(i).writeObject(msg);
				
				sInputArray.get(i).close();						
				sOutputArray.get(i).close();		
				serverConnectionSockets.get(i).close();

				sInputArray.remove(i);
				sOutputArray.remove(i);	
				serverConnectionSockets.remove(i);
				
			}
		}

		
	}
	
	public static void shutdown(NodeInfo currentNode) throws UnknownHostException, IOException {
		
		Scanner inputReader = new Scanner(System.in);  // Create a Scanner object for keyboard inputs		
		
		if(serverConnectionSockets.size() == 0) {
			
			System.out.println("You need to join a chat first!");
			return;
			
		}
		
		//print all connects sockets to ask where to leave
		for(int i = 0; i < serverConnectionSockets.size(); i++) {
			
			System.out.println((i+1));
			System.out.println("IP: " + serverConnectionSockets.get(i).getInetAddress().getLocalHost());
			System.out.println("Port: " + serverConnectionSockets.get(i).getLocalPort());
			System.out.println();
			
		}

		System.out.println("Enter IP of node to leave: ");
		String ipToLeave = inputReader.nextLine();

		System.out.println("Enter IP of node to leave: ");
		int portToLeave = Integer.parseInt(inputReader.nextLine());;
		
		Message msg = new Message(Message.SHUTDOWN, currentNode.getName() + ": " + "SHUTDOWN");
		
		//receiver.stop();						
		
		for(int i = 0; i < serverConnectionSockets.size(); i++) {
			
			if(serverConnectionSockets.get(i).getLocalAddress().getLocalHost().toString().contains(ipToLeave) && serverConnectionSockets.get(i).getPort() == portToLeave){
				
				sOutputArray.get(i).writeObject(msg);
				
				sInputArray.get(i).close();						
				sOutputArray.get(i).close();		
				serverConnectionSockets.get(i).close();

				sInputArray.remove(i);
				sOutputArray.remove(i);	
				serverConnectionSockets.remove(i);
				
			}
		}
			
		System.exit(0);;

		
	}
	
	public static void shutdownAll(NodeInfo currentNode) throws IOException {
		
		if(serverConnectionSockets.size() == 0) {
			
			System.out.println("You need to join a chat first!");
			return;
			
		}
		
		Message msg = new Message(Message.SHUTDOWN_ALL, currentNode.getName() + ": " + "SHUTDOWN_ALL");
		
		//receiver.stop();						
		
		for(int i = 0; i < serverConnectionSockets.size(); i++) {
								
			sOutputArray.get(i).writeObject(msg);
			
			sInputArray.get(i).close();						
			sOutputArray.get(i).close();		
			serverConnectionSockets.get(i).close();

			sInputArray.remove(i);
			sOutputArray.remove(i);	
			serverConnectionSockets.remove(i);
				
		}
			
		System.exit(0);;

		
	}
	
	public static void sendMessage(NodeInfo currentNode, String message) throws IOException {
		
		Message msg = new Message(Message.NOTE, currentNode.getName() + ": " + message);
		
		for(int i = 0; i < serverConnectionSockets.size(); i++) {
			
			sOutputArray.get(i).writeObject(msg);
		
		}

		
	}
	
	//a function to run the node functionality
	public static void startPeer() throws IOException {
		
		Scanner inputReader = new Scanner(System.in);  // Create a Scanner object for keyboard inputs		
		ArrayList<String> clientNames = new ArrayList<String>(); //a list to store human names
		
		NodeInfo currentNode = new NodeInfo(listeningSocket.getInetAddress().getLocalHost().toString(), Integer.toString(listeningSocket.getLocalPort()));
		
		boolean shutdown = false;
		String join = "JOIN", leave = "LEAVE", shutDown = "SHUTDOWN", shutdownAll = "SHUTDOWN_ALL";
		
		//listen to commands forever, and break if the node is shutdown
		while (true) {
		
			//if the node is shutdown, exit the program for this node
			if(shutdown == true) {
				
				System.exit(0);
				
			}

			String cmd = readCommand();
							
			//handle the join message
			if (join.equalsIgnoreCase(cmd)) {
				
				joinChat(currentNode);
				
			}
			//check for leave command
			else if (leave.equalsIgnoreCase(cmd)) {
				
				leaveChat(currentNode);
				
				//continue the input loop and take next command
				continue;
				
			}
			//if shutdown command is give, same logic as leave, except that the program is exited after sending messages
			else if (shutDown.equalsIgnoreCase(cmd)) {
				
				shutdown(currentNode);
				
			}	
			//same logic as shutdown, just the message sent differes and shuts down every message receiving node
			else if (shutdownAll.equalsIgnoreCase(cmd)) {
				
				shutdownAll(currentNode);
				
			}		
			//all other messages are considered as notes, and are sent over to all the connected nodes
			else {
				
				sendMessage(currentNode, cmd);
				
			}			
					
		}			
		
	}
	
}