package dimmunix;

import java.util.Iterator;

public class ThreadNode extends Node {
	Thread thread;
	Position reqPos = null;
	Object yieldLock = new Object();
	
	Dimmunix dImmunix;
	
	Vector<ThreadNode> threadYields = new Vector<ThreadNode>(10);
	Vector<Position> posYields = new Vector<Position>(10);
	Signature yieldCauseTemplate = null;
	Instance templateInstance = new Instance();
	Instance yieldCause = new Instance();
	
	CallStack currentCallStack = new CallStack(10);	
	Vector<Iterator<LockGrant>> currentLockGrantsIterators = new Vector<Iterator<LockGrant>>(10);
	Vector<Vector<Position>> currentMatchingPositions = new Vector<Vector<Position>>(10);
	Vector<Position> currentPositionsToMatch = new Vector<Position>(10);
	Vector<Signature> currentTemplates = new Vector<Signature>(10);
	Vector<Integer> currentIndicesInTemplates = new Vector<Integer>(10);
	
	boolean reqPosInHistory = false;
	LockGrant currentLockGrant;
		
	Vector<LockGrant> preallocatedLockGrants = new Vector<LockGrant>(100);
	Vector<LockGrant> removedLockGrants = new Vector<LockGrant>(100);
	
	Vector<Event> lockOps = new Vector<Event>(100);
	
	int nSyncs = 0;
	
	boolean bypassAvoidance = false;
	
	public String toString() {
		return this.thread.getName();
	}
	
	public ThreadNode(Thread thread, Dimmunix dImmunix) {
		this.thread = thread;
		this.dImmunix = dImmunix;
		
		for (int i = 0; i < currentMatchingPositions.capacity(); i++) {
			currentMatchingPositions.add(new Vector<Position>(10));
		}
	}

	public int hashCode() {
		return (int)this.thread.getId();
	}

	public boolean equals(Object n) {
		if (n == null || !(n instanceof ThreadNode))
			return false;
		return this.thread == ((ThreadNode)n).thread;
	}
	
	boolean allYieldsGrey() {
		for (int i = 0; i < threadYields.size(); i++) {
			if (threadYields.get(i).color != Color.GREY)
				return false;			
		}
		return true;
	}		
	
	void resetYieldCauseTo(Instance instance) {
		this.yieldCause.setSize(instance.size());
		for (int i = 0; i < instance.size(); i++)
			this.yieldCause.lockGrants.set(i, instance.lockGrants.get(i));
		this.yieldCause.template = instance.template;
	}
	
	void resumeFromLivelock() {
		synchronized (yieldLock) {
			yieldLock.notify();
			yieldCause.clear();
			this.bypassAvoidance = true;
		}
		threadYields.clear();
		posYields.clear();
		yieldCauseTemplate = null;
	}
	
	boolean isNotified() {
		return this.yieldCause.isEmpty();
	}
}
