package dimmunix.external;

import java.util.Collections;
import java.util.List;

import dimmunix.Vector;

public class History {
	public static final History instance = new History();
	
	private History() {		
	}
	
	private Vector<Signature> signatures = new Vector<Signature>();
	
	public void addSignature(Signature sig) {
		this.signatures.add(sig);
	}
	
	public List<Signature> getSignatures() {
		return Collections.unmodifiableList(this.signatures);
	}
}
