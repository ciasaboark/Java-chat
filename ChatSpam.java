import java.util.*;
import java.io.*;
import java.net.*;


public class ChatSpam {
	private static String address = "localhost";
	private static int port = 1850;
	
	
	public static void main (String[] argv) throws IOException,InterruptedException {
		for (int i = 0; i < 500; i++) {
			Socket server = new Socket( address, port );
			//InputStream in = server.getInputStream();
			OutputStream out = server.getOutputStream();
			PrintWriter pout = new PrintWriter( out, true);
			
			System.out.println("Connecting fake user user" + i);
			pout.println("user" + i);
			//Thread.sleep(300);
			//System.out.println("Disconnecting fake user user" + i);
			pout.println("user" + i);
			pout.println("Hi, I am my name is user" + i); 
			pout.println("DISCONNECT");
			//Thread.sleep(1000);
			server.close();
		}
	}
}
			
