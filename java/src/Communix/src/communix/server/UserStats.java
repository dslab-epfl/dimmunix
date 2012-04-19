package communix.server;

import communix.Signature;
import communix.Vector;

public class UserStats {
	private long timeLastUpdate = 0;
	
	private Vector<Signature> signatures = new Vector<Signature>();
	
	public synchronized boolean isTimeToUpdate() {
		long now = System.currentTimeMillis();
		boolean ok = (now- this.timeLastUpdate) >= Server.UPDATE_PERIOD_MSEC;
		if (ok) {
			this.timeLastUpdate = now; 
		}
		return ok;
	}
	
	public synchronized boolean addSignature(Signature newSig) {
		for (Signature s: this.signatures) {
			if (s.partialOverlap(newSig)) {
				return false;
			}
		}
		this.signatures.add(newSig);
		return true;
	}
}
