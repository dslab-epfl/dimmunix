package dimmunix.hybrid;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import dimmunix.Vector;

public class History {
	
	public static final History instance = new History(); 
	
	private History() {		
	}
	
	private Vector<Signature> signatures = new Vector<Signature>();
	
	public void addSignature(Signature sig) {
		this.signatures.add(sig);
	}
	
	public void load() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("hybrid_deadlocks"));

			String line;
			while ((line = br.readLine()) != null) {
				Signature sig = new Signature(line);
				this.signatures.add(sig);
			}
			
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	public void save() {
		try {
			PrintWriter pw = new PrintWriter("hybrid_deadlocks");
			
			for (Signature sig: this.signatures) {
				pw.println(sig);
			}
			
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean enter(ThreadNode t, StackTraceElement pos) {
		boolean matched = false;
		for (Signature sig: this.signatures) {
			if (sig.enter(t, pos)) {
				matched = true;
			}
		}
		return matched;
	}
	
	public void exit(ThreadNode t, StackTraceElement pos) {
		for (Signature sig: this.signatures) {
			sig.exit(t, pos);
		}		
	}
	
	public boolean isInstantiated(ThreadNode t) {
		for (Signature sig: this.signatures) {
			if (sig.isInstantiated(t)) {
				return true;
			}
		}
		return false;
	}
}
