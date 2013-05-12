//Jonathan Nelson
//ChatServerConnection.java

/********* TODO
 * 	-the socket timeout should only be expanded after the connection has been verified
 * 	-the incomming messages should be translated to a better syntax so the clients
 * 		can better choose how each message is displayed (highlighting, notifications, etc).
 * 		Perhaps a simple XML-like markup would make sense
 * 	-having a table of reserved usernames might come in handy.  This would probably require
 * 		an additional PASSWORD prompt from the server
 * 	-the control messages should require the user client to respond with the appropriate
 * 		command prefix (i.e. server:USER, client:USER a_user_name).  This could help the
 * 		server to be sure that only a proper client is connecting, and not some process sending
 * 		random bytes to the server.
 * 	-performace might be improved by moving broadcast() to the super ChatServer class, since
 * 		only one copy would be needed.
 * 
 * 
 *********/


import java.util.*;
import java.io.*;
import java.net.*;


class ChatServerConnection extends ChatServer implements Runnable  {
	private Socket client;
	private String user;
	
	@SuppressWarnings("unchecked")
	public ChatServerConnection(Socket newClient)  {
		try {
			
			this.client = newClient;
			client.setKeepAlive(true);
			//I'm not really sure if this 20 minute timeout applies only to
			//+ incomming traffic, or if it is reset by outgoing broadcasts
			client.setSoTimeout(1200000);
			
			System.out.println("New connection from " + client.getInetAddress() );
			
			//Very basic authentication, ask the client for a username, then
			//+ allow 6 seconds for a username response.  If no response is incomming
			//+ then the connection is dropped.
			sendMsg("USER");
			BufferedReader in = new BufferedReader( new InputStreamReader( client.getInputStream() ));
			
			for (int i = 0; i < 24 && user == null; i++) {
				//check for a username at quarter second intervals for 6 seconds
				if (in.ready()) {
					//replace spaces with underscores so LIST can be whitespace seperated,
					//+ then strip undesireable characters
					user = in.readLine().trim().toLowerCase();
					user = user.replaceAll(" ", "_");
					user = user.replaceAll("^", "");
					
					//check if that username is already in use
					//to keep the handshake process relatively fast we operate on a cloned copy of the client list
					//+ instead of using the synchronized access methods.  This could potentially lead to data integrity
					//+ problems if multiple users try to connect using the same user name at the same.
					for (ChatServerConnection myConnection : (ArrayList<ChatServerConnection>)ChatServer.getClientList().clone()) {
						if ( myConnection.getUser().equals(user)) {
							shutdown("BADUSER");
						}
					}
										
					if (user == null || user.equals("") || user.equals(" ")) {
						//blank or empty strings are not valid names
						shutdown("BADUSER");
					} else {
						this.sendMsg("WELCOME " + user);
						super.broadcast(user + " joined the chat");
						super.addClient(this);
						System.out.println(client.getInetAddress() + " handshake succeeded with user '" + user + "'");
						
						//send a list of currently connected clients
						ChatServer.broadcast(super.getUserList());
					}
				} else {
					Thread.sleep(250);
				}
			}
			
			if (user == null) {
				//the 6 second handshake has timed out without receiving a username
				System.out.println(client.getInetAddress() + " handshake failed");
				this.shutdown("TIMEOUT");
			}
			
		Thread.yield();
			
		//we assume that any exceptions mean that the connection is no longer valid
		} catch (SocketException e) {
			shutdown();
		} catch (IOException e) {
		} catch (InterruptedException e) {
		} finally {
		}
	}
	
	public String getUser() {
		return user;
	}
	
	public void sendMsg( String message ) throws IOException {
		PrintWriter pout = new PrintWriter( new OutputStreamWriter(client.getOutputStream(), "8859_1"), true);
		pout.println( message );
	}
	
	protected void shutdown() {
			shutdown("");
	}
	
	protected void shutdown(String reason) {
		try {
			sendMsg("GOODBYE " + reason);
			super.remClient(this);
			
			client.close();
			if (reason.equals("DISCONNECT") || reason.equals("")) {
				//there isn't any reason to broadcast a user list for server shutdowns
				//+ and failed handshakes
				super.broadcast(user + " left the chat.");
				super.broadcast(super.getUserList());
			}
		} catch (IOException e) {
		}
	}

	@Override			
	public void run() {
		//Thread.setPriority(4);
		try {
			BufferedReader in = new BufferedReader( new InputStreamReader( client.getInputStream() ));
			String message = "";
			
			while (message != null) {
				message = in.readLine().trim();
				
				
				//first we check for known commands, if nothing matches it must be a message
				//+ to be broadcast
				if (message.equals("DISCONNECT") || message == null ) {
					shutdown("DISCONNECT");
				} else if (message.equals("")) {
					//blank lines are ignored
				} else if (message.equals("HEARTBEAT") ) {
					sendMsg("HEARTBEAT");
				} else if (message.equals("GETLIST")) {
					//send a list of all connected users
					sendMsg(super.getUserList());
				} else {
					//pass to all other connections
					super.broadcast("<" +user + "> " + message);
				}
				
				//It's possible that a user could flood the server with messages,
				//+ sleeping for a bit between loops could help slow this down.
				Thread.yield();
				Thread.sleep(1);
				
				
			
			}
			
		//we assume that all exceptions mean that the connection is no longer valid.
		} catch (NullPointerException e) {
			shutdown();
		} catch (InterruptedException e) {
			shutdown();
		} catch (IOException e) {
			shutdown();
		}
	}		
}
