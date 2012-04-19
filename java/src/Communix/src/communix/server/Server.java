package communix.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import communix.LSLAESCrypto;
import communix.Service;
import communix.Signature;
import communix.SignatureDatabase;
import communix.Vector;

public class Server extends Service {
	
	public static final int PORT = 4567;
	
	public static final long UPDATE_PERIOD_MSEC = 1000;//(long)24* 3600* 1000; 
	
	private ThreadPoolExecutor threadPool;
	private ServerSocket serverSocket;
	
	private final HashMap<String, UserStats> userStatsMap;
	
	public Server() {
		try {
			this.serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.threadPool = new ThreadPoolExecutor(16, 32, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000000));		
		this.threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				r.run();
			}
		});
		
		this.database = new SignatureDatabase("server_Dimmunix.hist");
		
		this.userStatsMap = new HashMap<String, UserStats>();
	}
	
	public void run() {
		System.out.println("starting communix server");
		
		while (running) {
			try {
				final Socket clientSocket = this.serverSocket.accept();

				this.threadPool.execute(new Runnable() {
					private Socket client = clientSocket;
					
					public void run() {
						Server.this.processRequest(this.client);
					}					
				});				
			}
			catch (SocketException e) {
				if (this.running) {
					e.printStackTrace();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		} 
		
		System.out.println("server shutting down");
		this.threadPool.shutdown();
		try {
			this.threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		this.database.save(this.database.initialSize);
	}	
	
	public long runTest(int nReqs) {
		double tStart = System.nanoTime();
		
		final int nUsers = nReqs/ 100;
		final Vector<String> usersEnc = new Vector<String>(nUsers);
		for (int i = 0; i < nUsers; i++) {
			usersEnc.add(LSLAESCrypto.instance.encrypt("communix_user"+ i));
		}
		
		for (int i = 0; i < nReqs; i++) {
			final int round = i;
			this.threadPool.execute(new Runnable() {
				public void run() {
					String userId = LSLAESCrypto.instance.decrypt(usersEnc.get(round% nUsers));
					UserStats stats = Server.this.getUserStats(userId);
					
					Signature sig = Server.this.generateRandomSignature();
					stats.addSignature(sig);
					Server.this.database.addSignature(sig);
					
					stats.isTimeToUpdate();
					Server.this.iterateThroughDatabase();
					
					Server.this.nRequests.getAndAdd(2);
				}					
			});				
		} 
		
		this.threadPool.shutdown();
		try {
			this.threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		double tEnd = System.nanoTime();
		double secondsElapsed = (tEnd- tStart)/ 1000/ 1000/ 1000;
		double requestsPerSecond = this.nRequests.get()/ secondsElapsed;
		
		System.out.println(requestsPerSecond+ " req/sec");
		
		return (long)requestsPerSecond;
	}	
	
	private UserStats getUserStats(String userId) {
		synchronized (this.userStatsMap) {	
			UserStats stats = this.userStatsMap.get(userId);
			if (stats == null) {
				stats = new UserStats();
				this.userStatsMap.put(userId, stats);
			}
			return stats;
		}		
	}
	
	private void processRequest(Socket clientSocket) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));	
			String userId = LSLAESCrypto.instance.decrypt(br.readLine());
			UserStats userStats = this.getUserStats(userId);
			
			String request = br.readLine();
			StringTokenizer st = new StringTokenizer(request, " ");			
			String reqType = st.nextToken();
			
			if (reqType.equals("get")) {
				int startId = Integer.parseInt(st.nextToken());
				this.sendSignatures(clientSocket, userStats, startId);					
			}
			else if (reqType.equals("add")) {
				String sigStr = st.nextToken();
				try {
					Signature sig = new Signature(sigStr);
					this.addSignature(clientSocket, userStats, sig);											
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			br.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void iterateThroughDatabase() {
		int dbSize = this.database.size();
		List<Signature> dbSigs = this.database.getSignatures();
		
		for (int i = 0; i < dbSize; i++) {
			Signature sig = dbSigs.get(i);
		}
	}
	
	private void sendSignatures(Socket clientSocket, UserStats userStats, int startId) {
		try {			
			PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
			
			if (userStats.isTimeToUpdate()) {
				if (startId == this.database.size()) {
					pw.println("nothing");
				}
				else {
					int dbSize = this.database.size();
					List<Signature> dbSigs = this.database.getSignatures();
					
					for (int i = startId; i < dbSize; i++) {
						Signature sig = dbSigs.get(i);
						pw.println(sig);
					}
				}				
			}
			else {
				System.out.println("not time to update yet");
				pw.println("nothing");				
			}
			
			pw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addSignature(Socket clientSocket, UserStats userStats, Signature sig) {
		if (userStats.addSignature(sig)) {
			this.database.addSignature(sig);	
		}
		else {
			System.out.println("user already sent an overlapping signature");
		}
		
		try {
			PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
			
			pw.println("done");
			
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void kill() {
		this.running = false;
		try {			
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public void reset() {
		this.database.clear();
		this.userStatsMap.clear();
		
		this.nRequests.set(0);
		
		this.threadPool = new ThreadPoolExecutor(16, 32, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000000));		
		this.threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				r.run();
			}
		});
	}	
}
