package communix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import dimmunix.Configuration;
import dimmunix.deadlock.Signature;

public class Client {
	public static final int SERVER_PORT = 4567;
	public static final String TEST_ID = "communix_user";
	
	private Client() {
	}
	
	public static final Client instance = new Client();
	
	public void sendSignature(Signature dimmunixSig) {
		communix.Signature communixSig = dimmunixSig.toCommunixSignature();
		
		try {
			Socket serverSocket = new Socket(Configuration.instance.communixServer, SERVER_PORT);
			
			PrintWriter pw = new PrintWriter(serverSocket.getOutputStream(), true);
			pw.println(AES.instance.encrypt(TEST_ID));
			pw.println("add "+ communixSig);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			br.readLine();
			
			pw.close();
			br.close();
			serverSocket.close();			
		}
		catch (SocketException ex) {
			//try again
			this.sendSignature(dimmunixSig);
		}
		catch (Exception ex) {
		}
	}
}
