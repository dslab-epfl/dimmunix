package communix;

import communix.client.Client;
import communix.client.StressTest;
import communix.server.Server;

public class Communix {
	
	public static Service service = null;
	
	private static final String helpMessage = "need --server, --serverTest <nRounds> <nReqs>, --client <serverAddress>, or --test <serverAddress nWorkers nReqsPerWorker> as arguments";
	
	private static void startServiceAndWait() {
		service.start();
		try {
			service.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException(helpMessage);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Communix.service.kill();
			}
		});
		
		if (args[0].equals("--server")) {
			service = new Server();
			
			startServiceAndWait();
		}
		else if (args[0].equals("--serverTest")) {
			if (args.length < 3) {
				throw new IllegalArgumentException(helpMessage);
			}
			int nRounds = Integer.parseInt(args[1]);
			int nReqs = Integer.parseInt(args[2]);
			service = new Server();
			Server server = (Server)service;
			
			System.out.println("starting communix server test");
			
			long nReqsPerSec = 0;
			for (int i = 0; i < nRounds; i++) {
				nReqsPerSec += server.runTest(nReqs);
				server.reset();
			}			
			nReqsPerSec /= nRounds;
			
			System.out.println("server test done: "+ nReqsPerSec+ " req/sec");
		}
		else if (args[0].equals("--client")) {
			if (args.length < 2) {
				throw new IllegalArgumentException(helpMessage);
			}
			String serverAddress = args[1];
			service = new Client(serverAddress);
			
			startServiceAndWait();
		}
		else if (args[0].equals("--test")) {
			if (args.length < 4) {
				throw new IllegalArgumentException(helpMessage);
			}			
			String serverAddress = args[1];
			int nWorkers = Integer.parseInt(args[2]);
			int nReqsPerWorker = Integer.parseInt(args[3]);			
			service = new StressTest(serverAddress, nWorkers, nReqsPerWorker);
			
			startServiceAndWait();
		}
		else {
			throw new IllegalArgumentException(helpMessage);
		}		
	}
}
