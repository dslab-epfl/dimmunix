package dimmunix.init;

import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.loadtime.ExecutionPositions;

public class DimmunixInitDlcks {
	private class LockInfo {
		private final Object obj;		
		private volatile StackTraceElement acqPos;
		
		public LockInfo(Object obj) {
			this.obj = obj;
			this.acqPos = null;
		}
		
		public int hashCode() {
			return System.identityHashCode(obj);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof LockInfo) {
				LockInfo linfo = (LockInfo)obj;
				return this.obj == linfo.obj;
			}
			return false;
		}
	}
	
	private class ThreadInfo {
		private final long id;		
		private StackTraceElement reqPos;
		
		public ThreadInfo(long id) {
			this.id = id;
			this.reqPos = null;
		}
		
		public int hashCode() {
			return (int)id;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof ThreadInfo) {
				ThreadInfo tinfo = (ThreadInfo)obj;
				return this.id == tinfo.id;
			}
			return false;
		}
	}
	
	public static final DimmunixInitDlcks instance = new DimmunixInitDlcks();
	
	private DimmunixInitDlcks() {		
	}
	
	private ConcurrentHashMap<Integer, LockInfo> locks = new ConcurrentHashMap<Integer, LockInfo>();
	private ConcurrentHashMap<Long, ThreadInfo> threads = new ConcurrentHashMap<Long, ThreadInfo>();
	
	public LockInfo getLockInfo(Object x) {
		int id = System.identityHashCode(x);
		Integer idX = id;
		LockInfo l = this.locks.get(idX);
		if (l == null) {
			synchronized (this.locks) {
				l = this.locks.get(idX);
				if (l == null) {
					l = new LockInfo(x);
					this.locks.put(idX, l);
				}
			}
		}
		return l;
	}
	
	public ThreadInfo getThreadInfo(long id) {
		Long idX = id;
		ThreadInfo t = this.threads.get(idX);
		if (t == null) {
			synchronized (this.threads) {
				t = this.threads.get(idX);
				if (t == null) {
					t = new ThreadInfo(id);
					this.threads.put(idX, t);
				}
			}
		}
		return t;
	}
	
	public void init() {
		History.instance.load();
	}
	
	public void beforeLock(Object obj) {
		Thread t = Thread.currentThread();
		StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
		ThreadInfo tinfo = this.getThreadInfo(t.getId());
		
		History.instance.beforeLock(t, pos);
		
		tinfo.reqPos = pos;
	}
	
	public void afterLock(Object obj) {
		Thread t = Thread.currentThread();
		ThreadInfo tinfo = this.getThreadInfo(t.getId());
		LockInfo linfo = this.getLockInfo(obj);
		
		linfo.acqPos = tinfo.reqPos; 
	}
	
	public void beforeUnlock(Object obj) {
		Thread t = Thread.currentThread();
		LockInfo linfo = this.getLockInfo(obj);
		
		History.instance.beforeUnlock(t, linfo.acqPos);
	}
	
	public void beforeInit(String cls) {
		History.instance.beforeInit(Thread.currentThread(), cls);
	}
	
	public void afterInit(String cls) {
		History.instance.afterInit(cls);
	}
	
	public void shutDown() {
		DeadlockDetector.instance.run();
		
		History.instance.save();
	}
}
