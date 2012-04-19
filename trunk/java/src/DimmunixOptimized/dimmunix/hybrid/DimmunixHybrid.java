package dimmunix.hybrid;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.aspectj.weaver.loadtime.ExecutionPositions;
import dimmunix.Pair;
import dimmunix.Vector;

public class DimmunixHybrid {
	
	public static final DimmunixHybrid instance = new DimmunixHybrid();
	
	private Field syncFieldReadLock;
	private Field syncFieldWriteLock;
	
	private DimmunixHybrid() {	
		try {
			syncFieldReadLock = ReentrantReadWriteLock.ReadLock.class.getDeclaredField("sync");
			syncFieldReadLock.setAccessible(true);
			syncFieldWriteLock = ReentrantReadWriteLock.WriteLock.class.getDeclaredField("sync");
			syncFieldWriteLock.setAccessible(true);
		} catch (Exception e) {
			syncFieldReadLock = null;
			syncFieldWriteLock = null;
			e.printStackTrace();
		} 
	}
	
	private HashMap<Long, ThreadNode> threads = new HashMap<Long, ThreadNode>();
	private HashMap<Integer, ResourceNode> resources = new HashMap<Integer, ResourceNode>();
	
	private ThreadNode getThreadNode(long tid) {
		Long idX = tid;
		ThreadNode t = this.threads.get(idX);
		if (t == null) {
			t = new ThreadNode(tid);
			this.threads.put(idX, t);
		}
		return t;
	}
	
	private ResourceNode getResourceNode(Object obj, int permits) {
		Integer id = System.identityHashCode(obj);
		ResourceNode r = this.resources.get(id);
		if (r == null) {
			if (obj instanceof ReentrantLock) {
				r = ResourceNode.newMutex(obj);
			}
			if (obj instanceof AbstractQueuedSynchronizer) {
				r = ResourceNode.newRWLock(obj);
			}
			if (obj instanceof Semaphore) {
				r = ResourceNode.newSemaphore(obj, permits);
			}
			this.resources.put(id, r);
		}
		return r;
	}
	
	private void checkForDeadlocks() {
		synchronized (this) {
			HashSet<ThreadNode> dlckThreads = new HashSet<ThreadNode>();
			for (ThreadNode t: this.threads.values()) {
				if (dlckThreads.contains(t)) {
					continue;
				}
				Pair<Signature, Vector<ThreadNode>> dlck = t.getDeadlock();
				if (dlck != null) {
					System.out.println("hybrid deadlock found !");
					Signature sig = dlck.v1;
					dlckThreads.addAll(dlck.v2);
					
					History.instance.addSignature(sig);
				}
			}			
		}
	}
	
	public void beforeLock(ReentrantLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
			ResourceNode rnode = this.getResourceNode(obj, 0);
			
			tnode.mutexReq(rnode, pos);		
			
			this.enter(tnode, pos);			
		}
	}
	
	public void afterLock(ReentrantLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(obj, 0);
			
			rnode.lock(tnode);					
		}
	}
	
	public void beforeUnlock(ReentrantLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
//			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(obj, 0);
			
			this.exit(t, rnode.getAcqPosition());
			
			rnode.unlock();					
		}
	}	
	
	public void beforeLockr(ReentrantReadWriteLock.ReadLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
			ResourceNode rnode = this.getResourceNode(this.getReadLockObj(obj), 0);
			
			tnode.readLockReq(rnode, pos);
			
			this.enter(tnode, pos);			
		}
	}
	
	public void afterLockr(ReentrantReadWriteLock.ReadLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(this.getReadLockObj(obj), 0);

			rnode.lockr(tnode);			
		}
	}
	
	public void beforeUnlockr(ReentrantReadWriteLock.ReadLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(this.getReadLockObj(obj), 0);
			
			this.exit(t, rnode.getAcqPosition(tnode));
			
			rnode.unlockr(tnode);			
		}
	}		
	
	public void beforeLockw(ReentrantReadWriteLock.WriteLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
			ResourceNode rnode = this.getResourceNode(this.getWriteLockObj(obj), 0);
			
			tnode.writeLockReq(rnode, pos);
			
			this.enter(tnode, pos);			
		}
	}
	
	public void afterLockw(ReentrantReadWriteLock.WriteLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(this.getWriteLockObj(obj), 0);
			
			rnode.lockw(tnode);			
		}
	}
	
	public void beforeUnlockw(ReentrantReadWriteLock.WriteLock obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(this.getWriteLockObj(obj), 0);
			
			this.exit(t, rnode.getAcqPosition(tnode));
			
			rnode.unlockw(tnode);			
		}
	}			
	
	public void beforeAcquire(Semaphore obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
			ResourceNode rnode = this.getResourceNode(obj, obj.availablePermits());
			
			tnode.semaphoreReq(rnode, pos);
			
			this.enter(tnode, pos);			
		}
	}
	
	public void afterAcquire(Semaphore obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(obj, obj.availablePermits());
			
			rnode.acquire(tnode);			
		}
	}
	
	public void beforeRelease(Semaphore obj) {
		synchronized (this) {
			Thread t = Thread.currentThread();
			ThreadNode tnode = this.getThreadNode(t.getId());
			ResourceNode rnode = this.getResourceNode(obj, obj.availablePermits());
			
			this.exit(t, rnode.getAcqPosition(tnode));
			
			rnode.release(tnode);			
		}
	}	
	
	public void init() {
		History.instance.load();
	}
	
	public void shutDown() {
//		System.out.println("checking for hybrid deadlocks");
		this.checkForDeadlocks();
		
		History.instance.save();		
	}
	
	private Object getWriteLockObj(ReentrantReadWriteLock.WriteLock obj) {
		Object l = null;
		if (syncFieldWriteLock != null) {
			try {
				l = syncFieldWriteLock.get(obj);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return l;
	}
	
	private Object getReadLockObj(ReentrantReadWriteLock.ReadLock obj) {
		Object l = null;
		if (syncFieldReadLock != null) {
			try {
				l = syncFieldReadLock.get(obj);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return l;
	}
	
	private void enter(ThreadNode t, StackTraceElement pos) {
		boolean matched = History.instance.enter(t, pos);
		
		if (!matched) {
			return;
		}
		
		while (History.instance.isInstantiated(t)) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void exit(Thread t, StackTraceElement pos) {
		ThreadNode tnode = this.getThreadNode(t.getId());
		History.instance.exit(tnode, pos);
		
		boolean posInInst = tnode.removeInstancePosition(pos);
		if (posInInst) {
			if (!tnode.isPositionInInstances(pos)) {
				this.notifyAll();				
			}
		}		
	}
}
