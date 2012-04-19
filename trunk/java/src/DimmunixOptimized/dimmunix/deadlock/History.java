package dimmunix.deadlock;

import java.util.concurrent.ConcurrentLinkedQueue;

public class History {
	public ConcurrentLinkedQueue<Signature> historyQueue = new ConcurrentLinkedQueue<Signature>();
	Signature[] historyMap = new Signature[10000];
	public int sigIdMax = 0;
	
	public void add(Signature sig) {
		historyQueue.add(sig);
		historyMap[sig.id] = sig;
		if (sig.id > sigIdMax) {
			sigIdMax = sig.id;
		}
	}
	
	public void remove(Signature sig) {
		this.historyQueue.remove(sig);
	}
	
	public int size() {
		return historyQueue.size();
	}
	
	public boolean merge(Signature sig) {
		for (Signature s: this.historyQueue) {
			if (s.merge(sig)) {
				return true;
			}
		}
		return false;
	}
}
