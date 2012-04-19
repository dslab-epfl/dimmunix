package dimmunix.condvar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.aspectj.weaver.loadtime.ExecutionPositions;
import org.aspectj.weaver.loadtime.SyncPositions;

import dimmunix.Vector;

public class History {
	public static final History instance = new History();
	
	private final Vector<Signature> signatures = new Vector<Signature>();
	
	public void addSignature(Signature sig) {
		this.signatures.add(sig);
	}
	
	private History() {		
	}
	
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("blocked_notifications"));
			String line;
			
			while ((line = br.readLine()) != null) {
				this.signatures.add(new Signature(line));
			}
			br.close();
		} catch (Exception e) {
		}	
	}
	
	public boolean containsWaitPosition(StackTraceElement pos) {
		for (Signature sig: this.signatures) {
			if (sig.waitPos.equals(pos)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsLockBeforeWaitPosition(StackTraceElement pos) {
		for (Signature sig: this.signatures) {
			if (sig.syncBeforeWaitPos.equals(pos)) {
				return true;
			}
		}
		return false;		
	}
	
	public void addLockBeforeNotifyPosition(StackTraceElement waitPos, StackTraceElement lbnPos) {
		for (Signature sig: this.signatures) {
			if (sig.waitPos.equals(waitPos)) {
				sig.addLockBeforeNotifyPosition(lbnPos);
			}
		}
	}
	
	public void addLockBeforeNotifyPositions(StackTraceElement waitPos, Collection<StackTraceElement> lbnPos) {
		for (Signature sig: this.signatures) {
			if (sig.waitPos.equals(waitPos)) {
				sig.addLockBeforeNotifyPositions(lbnPos);
			}
		}
	}	
	
	public void save() {
		try {
			PrintWriter pw = new PrintWriter("blocked_notifications");			
			for (Signature sig: this.signatures) {
				pw.println(sig);
				
				SyncPositions.add(sig.syncBeforeWaitPos);
				ExecutionPositions.instance.addPosition(sig.syncBeforeWaitPos);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public List<Signature> getSignatures() {
		return Collections.unmodifiableList(this.signatures);
	}
}