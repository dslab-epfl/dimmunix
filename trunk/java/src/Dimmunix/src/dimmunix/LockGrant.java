package dimmunix;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockGrant {
	ThreadNode thread;
	LockNode lock;
	Position position;
	volatile int n = 0;
	ConcurrentLinkedQueue<ThreadNode> yielders = new ConcurrentLinkedQueue<ThreadNode>();
	ReentrantReadWriteLock yieldersLock = new ReentrantReadWriteLock();
	volatile long time = 0; 
	
	public LockGrant(ThreadNode thread, LockNode lock, Position position) {
		this.thread = thread;
		this.lock = lock;
		this.position = position;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof LockGrant))
			return false;
		LockGrant lg = (LockGrant)obj;
		return thread == lg.thread && lock == lg.lock;
	}
	
	boolean disjoint(LockGrant lg) {
		return thread != lg.thread && (lg.lock == null || lock != lg.lock);
	}
}
