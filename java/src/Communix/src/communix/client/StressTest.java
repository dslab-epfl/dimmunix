package communix.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import communix.LSLAESCrypto;
import communix.Signature;
import communix.SignatureDatabase;
import communix.Vector;
import communix.server.Server;


public class StressTest extends Client {
	
	private final int nWorkers;
	private final int nReqsPerWorker;
	private final CyclicBarrier barrier;
	
	public StressTest(String serverAddress, int nWorkers, int nReqsPerWorker) {
		this.serverAddress = serverAddress;
		this.nWorkers = nWorkers;
		this.nReqsPerWorker = nReqsPerWorker;
		this.barrier = new CyclicBarrier(nWorkers);
		
		this.database = new SignatureDatabase();
	}
	
	private void addSignature(String userId, Signature sig) {
		try {
			Socket serverSocket = new Socket(this.serverAddress, Server.PORT);
			
			PrintWriter pw = new PrintWriter(serverSocket.getOutputStream(), true);
			pw.println(LSLAESCrypto.instance.encrypt(userId));
			pw.println("add "+ sig);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			br.readLine();
			
			pw.close();
			br.close();
			serverSocket.close();			
		}
		catch (SocketException ex) {
			//retry 
			this.addSignature(userId, sig);
		}
		catch (Exception ex) {
		}
	}
	
	protected void getNewSignatures(String userId) {
		try {
			Socket serverSocket = new Socket(this.serverAddress, Server.PORT);
			
			PrintWriter pw = new PrintWriter(serverSocket.getOutputStream(), true);
			pw.println(LSLAESCrypto.instance.encrypt(userId));
			pw.println("get 0");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			while (true) {
				String sigStr = br.readLine();
				if (sigStr == null) {
					break;
				}
				if (sigStr.equals("nothing")) {
					break;
				}
//				try {
//					Signature sig = new Signature(sigStr);
//					this.database.addSignature(sig);						
//				}
//				catch (Exception ex) {
//					ex.printStackTrace();
//				}
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
	
	private void test() {
		for (int i = 0; i < this.nReqsPerWorker && this.running; i++) {
			String userId = "user"+ this.random.nextInt();
			this.addSignature(userId, this.generateRandomSignature());				
			this.getNewSignatures(userId);
			this.nRequests.getAndAdd(2);
		}
	}
	
	public void run() {
		System.out.println("starting stress test");
		double tStart = System.nanoTime();
		
		Vector<Thread> workers = new Vector<Thread>(this.nWorkers);
		for (int i = 0; i < this.nWorkers; i++) {
			workers.add(new Thread("worker "+ i) {
				public void run() {
					try {
						StressTest.this.barrier.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
					StressTest.this.test();
				}
			});
		}
		
		for (Thread w: workers) {
			w.start();
		}
		
		for (Thread w: workers) {
			try {
				w.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		double tEnd = System.nanoTime();
		double secondsElapsed = (tEnd- tStart)/ 1000/ 1000/ 1000;
		double requestsPerSecond = this.nRequests.get()/ secondsElapsed;
		System.out.println("stress test done, "+ requestsPerSecond+ " req/sec");
	}
}
