package dimmunix.init;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import dimmunix.Vector;

public class History {
	private Vector<Signature> signatures = new Vector<Signature>();
	
	public static final History instance = new History();
	
	private History() {		
	}
	
	public void load() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("init_deadlocks"));
			
			String line;
			while ((line = br.readLine()) != null) {
				this.signatures.add(new Signature(line));
			}
			
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	public void save() {
		try {
			PrintWriter pw = new PrintWriter("init_deadlocks");
			
			for (Signature sig: this.signatures) {
				pw.println(sig);
			}
			
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void beforeInit(Thread t, String cl) {
		for (Signature sig: this.signatures) {
			if (sig.className.equals(cl)) {
				synchronized (sig) {
					sig.startInit(t);					
				}
			}
		}
		
		while (true) {
			boolean instanceFound = false;
			for (Signature sig: this.signatures) {
				if (sig.className.equals(cl)) {
					instanceFound = this.checkInstances(sig);
				}
			}
			
			if (!instanceFound) {
				break;
			}
		}
	}
	
	public void afterInit(String cl) {
		for (Signature sig: this.signatures) {
			if (sig.className.equals(cl)) {
				synchronized (sig) {
					sig.stopInit();
					if (!sig.isInstantiated()) {
						sig.notifyAll();
					}
				}
			}
		}
	}
	
	public void beforeLock(Thread t, StackTraceElement pos) {
		for (Signature sig: this.signatures) {
			if (!sig.isInitialized() && sig.lockPos.equals(pos)) {
				synchronized (sig) {
					sig.enter(t);					
				}
			}			
		}
		while (true) {
			boolean instanceFound = false;
			for (Signature sig: this.signatures) {
				if (!sig.isInitialized() && sig.lockPos.equals(pos)) {
					instanceFound = this.checkInstances(sig);
				}
			}
			
			if (!instanceFound) {
				break;
			}
		}				
	}
	
	public void beforeUnlock(Thread t, StackTraceElement pos) {
		for (Signature sig: this.signatures) {
			if (!sig.isInitialized() && sig.lockPos.equals(pos)) {
				synchronized (sig) {
					sig.exit(t);
					if (!sig.isInstantiated()) {
						sig.notifyAll();
					}
				}
			}
		}		
	}
	
	private boolean checkInstances(Signature sig) {
		boolean instanceFound = false;
		synchronized (sig) {
			if (sig.isInstantiated()) {
				instanceFound = true;
				try {
					sig.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return instanceFound;
	}
	 
	public void addSignature(Signature sig) {
		this.signatures.add(sig);
	}
}
