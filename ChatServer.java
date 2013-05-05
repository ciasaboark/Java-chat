//Jonathan Nelson
//ChatServer.java

/*****
 * ChatServer is the small implementation of a client/server chat program.
 * ChatServer maintains a list of all connected clients and spawns a new
 * ChatServerConnection thread to handle the network I/O for each connecting
 * client.
 * 
 * In order for a new client to successfully connect to the server they must
 * connect to the correct port (default 1850).  The server will then send the
 * USER command and wait 6 seconds for a response containing the desired username
 * string.  Once this handshake is complete all subsequent messages will be
 * broadcast to all connected clients.  The server uses a few commands to let the
 * client know of special states:
 * 
 * USER :the server has received the connection request and is awating a username
 * 			response
 * LIST userlist :a whitespace seperated list of all connected users.  This command
 * 			will be sent whenever a client connects or disconnects.
 * WELCOME username :the server has received the username and the handshake is complete
 * 			Note that the username returned may be different from the username requested.
 * 			In general whitespace will be converted to underscores, and some characters
 * 			may be stripped.
 * GOODBYE optional_reason :the server has closed the connection.  The GOODBYE
 * 			command may be followed by a more spefific reason:
 * 					SHUTDOWN :the server is shutting down
 * 					TIMEOUT :the handshake procedure timed out
 * 					BADUSER :the reqested username is already in use
 * 					DISCONNECT :the disconnect message was received and the server
 * 							closed the connection.
 * HEARTBEAT :a heartbeat is sent only in response to a client sending a heartbeat message
 * 
 * The server will also listen for commands from the client.  Currently supported
 * commands are limited to:
 * 
 * HEARTBEAT :Used to maintain a connection to the server.  The server typically
 * 			maintains the connection for 20 minutes without input from the client.
 * 			Server will respond with HEARTBEAT.
 * GETLIST :returns the LIST command.  There is no guarentee that LIST will be the very
 * 			next message received.
 * DISCONNECT :client is shutting down the connection.  Server will respond with GOODBYE
 * 			DISCONNECT
 * 
 * 
 * 
 */

import java.util.*;
import java.io.*;
import java.net.*;

public class ChatServer {
	
	private static ArrayList<ChatServerConnection> clientList = new ArrayList<ChatServerConnection>();
	private static int port = 1850;
	
	protected static ThreadGroup clientThreads = new ThreadGroup("Clients");
	
	protected synchronized void addClient(ChatServerConnection newCon) {
		clientList.add( newCon );
	}
		
	protected  synchronized void remClient(ChatServerConnection newCon) {
		clientList.remove( newCon );
	}
	
	protected static synchronized  ArrayList<ChatServerConnection> getClientList() {
		return clientList;
	}
	
	@SuppressWarnings("unchecked")
	protected static synchronized  String getUserList() {
		StringBuilder sb = new StringBuilder("LIST ");
		for (ChatServerConnection myConnection : (ArrayList<ChatServerConnection>)clientList.clone()) {
			sb.append( myConnection.getUser() + " ");
		}
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	protected static synchronized  void broadcast(String message) throws IOException {
		for (ChatServerConnection myConnection : (ArrayList<ChatServerConnection>)clientList.clone()) {
			myConnection.sendMsg(message);
		}
	}
	
	public static void main( String[] argv) {
		
		//the shutdown hook is used to notify all connected clients that the server is going down
		Runtime.getRuntime().addShutdownHook( new Thread() {
			//since all connection threads that have finished the handshake process
			//+ will sleep for a max of .5 seconds we dont need to worry about
			//+ interrupting them to send the message
			
			//clone() returns an Object, not an Vector.  It might be cleaner to use syncrhonization
			//+ to avoid ConcurrentModification exceptions, but since the server is shutting down
			//+ we don't want to wait for any threads to finish iterating over the list.
			@SuppressWarnings("unchecked")
			public void run() {
				for (ChatServerConnection myConnection: (ArrayList<ChatServerConnection>)clientList.clone()) {
					try {
						myConnection.sendMsg("Server is shutting down, goodbye");
					
						//This second message should be caught by the client so it
						// + knows to shutdown its connection
						myConnection.shutdown("SHUTDOWN");
					} catch (IOException e) {
					}
				}
			}
		});
		
		try {
			//start a listener on port 1850
			System.out.println("Starting new server on port " + port);
			//Limit the number of backlogged connections to 3, this should help reduce the chance
			//+ of the server being spammed
			ServerSocket listener = new ServerSocket(port, 3);
			
			//Thread.setPriority(6);
			
			//main loop
			System.out.println("Listening...\n");
			while (true) {
				new Thread(clientThreads, new ChatServerConnection( listener.accept() )).start();
				Thread.yield();
				//Thread.sleep(300);
			}
		} catch (IOException e) {
		} //catch (InterruptedException e) {
		//}
	}
}
