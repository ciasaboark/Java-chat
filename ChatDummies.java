import java.util.*;
import java.io.*;
import java.net.*;


public class ChatDummies {
	private static String address = "localhost";
	private static int port = 1850;
	
	public ChatDummies() throws IOException,InterruptedException {
		for (int i = 0; i < 200; i++) {
			Socket server = new Socket( address, port );
			//InputStream in = server.getInputStream();
			new Thread(new DummyConnection(server, "name"+i)).start();
			
	
		}
	}
	
	public static void main (String[] argv) throws IOException,InterruptedException {
		new ChatDummies();
		while (true){}
	}

	private class DummyConnection implements Runnable {
		public DummyConnection (Socket mySocket, String name) throws IOException {
			//just connect, send name, then wait
			OutputStream out = mySocket.getOutputStream();
			PrintWriter pout = new PrintWriter( out, true);
			System.out.println("Connecting fake user user" + name);
			pout.println(name);
			//Thread.sleep(300);
			//System.out.println("Disconnecting fake user user" + i);
			//pout.println("user" + i);
			pout.println("Hi, I am my name is user " + name); 
			//Thread.sleep(1000);	
		}
		
		public void run() {
			while (true) {}
		}
	}

}
			
