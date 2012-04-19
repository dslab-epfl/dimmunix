package dimmunix.deadlock;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedSemaphore extends Semaphore {
	private ReentrantLock lock = new ReentrantLock();
	private final int capacity;
	
	public LockNode lockNode;
	
	public BoundedSemaphore(int permits) {
		super(permits);
		this.capacity = permits;
		this.lockNode = new LockNode(System.identityHashCode(this)); 
	}
	
	public BoundedSemaphore(int permits, boolean fair) {
		super(permits, fair);
		this.capacity = permits;
	}
	
	public void acquire() {
		try {
			super.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void release() {
		try {
			this.lock.lock();
			if (this.availablePermits() < this.capacity) {
				super.release();
			}
		}
		finally {
			this.lock.unlock();
		}
	}
}
