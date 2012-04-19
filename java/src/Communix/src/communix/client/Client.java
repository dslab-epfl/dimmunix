package communix.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import communix.LSLAESCrypto;
import communix.Service;
import communix.Signature;
import communix.SignatureDatabase;
import communix.server.Server;

public class Client extends Service {
	
	protected String serverAddress;
	public static final String TEST_ID = "communix_user";
	
	public Client(String serverAddress) {
		this.serverAddress = serverAddress;
		
		this.database = new SignatureDatabase("client_Dimmunix.hist");
	}
	
	protected Client() {
		this.serverAddress = null;
	}
	
	protected void getNewSignatures(String userId) {
		try {
			Socket serverSocket = new Socket(this.serverAddress, Server.PORT);
			
			PrintWriter pw = new PrintWriter(serverSocket.getOutputStream(), true);
			pw.println(LSLAESCrypto.instance.encrypt(userId));
			pw.println("get "+ this.database.size());
			
			BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			while (true) {
				String sigStr = br.readLine();
				if (sigStr == null) {
					break;
				}
				if (sigStr.equals("nothing")) {
					break;
				}
				try {
					Signature sig = new Signature(sigStr);
					this.database.addSignature(sig);						
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			pw.close();
			br.close();
			serverSocket.close();			
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}			
	}
	
	public void run() {
		System.out.println("starting communix client");			

		while (running) {
			try {
				Thread.sleep(Server.UPDATE_PERIOD_MSEC);				
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			int dbOldSize = this.database.size();
			
			this.getNewSignatures(TEST_ID);
			
			if (this.database.size() > dbOldSize) {
				this.database.save(dbOldSize);
			}
		}
		
		System.out.println("client shutting down");
	}
}
