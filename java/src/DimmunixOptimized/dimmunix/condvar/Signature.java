package dimmunix.condvar;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentSkipListSet;

import dimmunix.Util;

public class Signature {
	public final StackTraceElement waitPos;//waiter blocked here
	
	public final StackTraceElement syncBeforeWaitPos;//avoidance is done here
	
	public final StackTraceElement syncBeforeNotifyPos;//notifier blocked here
	
	private final HashSet<StackTraceElement> lockBeforeNotifyPositions = new HashSet<StackTraceElement>();
	
	private ConcurrentSkipListSet<ThreadInfo> yielders = new ConcurrentSkipListSet<ThreadInfo>();
	
	public Signature(StackTraceElement waitPos, StackTraceElement syncBeforeWaitPos, StackTraceElement syncBeforeNotifyPos) {
		this.waitPos = waitPos;
		this.syncBeforeWaitPos = syncBeforeWaitPos;
		this.syncBeforeNotifyPos = syncBeforeNotifyPos;
	}
	
	public Signature(String str) {
		StringTokenizer stok = new StringTokenizer(str, ",");
		
		this.waitPos = Util.parsePosition(stok.nextToken());
		this.syncBeforeWaitPos = Util.parsePosition(stok.nextToken());
		this.syncBeforeNotifyPos = Util.parsePosition(stok.nextToken());
		
		while (stok.hasMoreTokens()) {
			this.lockBeforeNotifyPositions.add(Util.parsePosition(stok.nextToken()));
		}
	}

	public int hashCode() {
		return this.waitPos.hashCode()^ this.syncBeforeWaitPos.hashCode()^ this.syncBeforeNotifyPos.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof Signature) {
			Signature sig = (Signature)obj;
			return this.waitPos.equals(sig.waitPos) && this.syncBeforeWaitPos.equals(sig.syncBeforeWaitPos) && this.syncBeforeNotifyPos.equals(sig.syncBeforeNotifyPos); 
		}
		return false;
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer(this.waitPos+ ","+ this.syncBeforeWaitPos+ ","+ this.syncBeforeNotifyPos);
		
		for (StackTraceElement p: this.lockBeforeNotifyPositions) {
			str.append(","+ p);
		}
		
		return str.toString();
	}
	
	public boolean addYielder(ThreadInfo t) {
		return this.yielders.add(t);
	}
	
	public boolean removeYielder(ThreadInfo t) {
		return this.yielders.remove(t);
	}
	
	public boolean containsYielder(ThreadInfo t) {
		return this.yielders.contains(t);
	}
	
	public int countYielders() {
		return this.yielders.size();
	}
	
	public void wakeUpYielders() {
		for (ThreadInfo t: this.yielders) {
			t.resumeAndWaitForYieldToFinish(this);
		}
	}
	
	public void addLockBeforeNotifyPosition(StackTraceElement p) {
		this.lockBeforeNotifyPositions.add(p);			
	}
	
	public void addLockBeforeNotifyPositions(Collection<StackTraceElement> positions) {
		this.lockBeforeNotifyPositions.addAll(positions);
	}
	
	public Set<StackTraceElement> getLockBeforeNotifyPositions() {
		return Collections.unmodifiableSet(this.lockBeforeNotifyPositions);
	}
}
