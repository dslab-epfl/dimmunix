package dimmunix.profiler;

import java.util.HashSet;

import dimmunix.SpecialStackTrace;
import dimmunix.Vector;

public class NonMutexStats {
	public static final NonMutexStats instance = new NonMutexStats();
	
	private class ThreadInfo {
		volatile int ncalls = 0;
		HashSet<Object> objs = new HashSet<Object>();
		HashSet<StackTraceElement> sites = new HashSet<StackTraceElement>();
		
		void addInfo(Object obj) {
			StackTraceElement pos = SpecialStackTrace.getFrame(CALL_STACK_OFFSET);
			
			ncalls++;
			objs.add(obj);
			sites.add(pos);
		}
	}
	
	public static class Result {
		int ncallsPerSec;
		int nthreads;
	}
	
	private static final int MAX_THREADS = 5000;
	private static final int CALL_STACK_OFFSET = 4;
	
	private Vector<ThreadInfo> waitStats = new Vector<NonMutexStats.ThreadInfo>(MAX_THREADS);
	private Vector<ThreadInfo> notifyStats = new Vector<NonMutexStats.ThreadInfo>(MAX_THREADS);
	private Vector<ThreadInfo> rwlockStats = new Vector<NonMutexStats.ThreadInfo>(MAX_THREADS);
	private Vector<ThreadInfo> semStats = new Vector<NonMutexStats.ThreadInfo>(MAX_THREADS);
	private Vector<ThreadInfo> flockStats = new Vector<NonMutexStats.ThreadInfo>(MAX_THREADS);
	
	private final long tstart;
	
	private NonMutexStats() {
		for (int i = 0; i < MAX_THREADS; i++) {
			this.waitStats.add(new ThreadInfo());
			this.notifyStats.add(new ThreadInfo());
			this.rwlockStats.add(new ThreadInfo());
			this.semStats.add(new ThreadInfo());
			this.flockStats.add(new ThreadInfo());
		}
		
		tstart = System.currentTimeMillis();
	}
	
	public void addWaitInfo(Object obj) {
		ThreadInfo tinfo = this.waitStats.get((int)Thread.currentThread().getId());
		tinfo.addInfo(obj);
	}
	
	public void addNotifyInfo(Object obj) {
		ThreadInfo tinfo = this.notifyStats.get((int)Thread.currentThread().getId());
		tinfo.addInfo(obj);
	}
	
	public void addRWLockInfo(Object obj) {
		ThreadInfo tinfo = this.rwlockStats.get((int)Thread.currentThread().getId());
		tinfo.addInfo(obj);
	}
	
	public void addSemaphoreInfo(Object obj) {
		ThreadInfo tinfo = this.semStats.get((int)Thread.currentThread().getId());
		tinfo.addInfo(obj);
	}
	
	public void addFileLockInfo(Object obj) {
		ThreadInfo tinfo = this.flockStats.get((int)Thread.currentThread().getId());
		tinfo.addInfo(obj);
	}
	
	public void collectStats() {
		float durationSec = (System.currentTimeMillis()- tstart)/ 1000; 
		
		float nwaitCallsPerSec = 0;
		int nwaitThreads = 0;
		HashSet<Object> waitObjs = new HashSet<Object>();
		HashSet<StackTraceElement> waitSites = new HashSet<StackTraceElement>();		
		for (ThreadInfo tinfo: this.waitStats) {
			if (tinfo.ncalls > 0) {
				nwaitThreads++;
				nwaitCallsPerSec += tinfo.ncalls;
				waitObjs.addAll(tinfo.objs);
				waitSites.addAll(tinfo.sites);
			}
		}
		nwaitCallsPerSec /= durationSec;
		
		float nnotifyCallsPerSec = 0;
		int nnotifyThreads = 0;
		HashSet<Object> notifyObjs = new HashSet<Object>();
		HashSet<StackTraceElement> notifySites = new HashSet<StackTraceElement>();		
		for (ThreadInfo tinfo: this.notifyStats) {
			if (tinfo.ncalls > 0) {
				nnotifyThreads++;
				nnotifyCallsPerSec += tinfo.ncalls;
				notifyObjs.addAll(tinfo.objs);
				notifySites.addAll(tinfo.sites);
			}
		}
		nnotifyCallsPerSec /= durationSec;
		
		float nrwlockCallsPerSec = 0;
		int nrwlockThreads = 0;
		HashSet<Object> rwlockObjs = new HashSet<Object>();
		HashSet<StackTraceElement> rwlockSites = new HashSet<StackTraceElement>();		
		for (ThreadInfo tinfo: this.rwlockStats) {
			if (tinfo.ncalls > 0) {
				nrwlockThreads++;
				nrwlockCallsPerSec += tinfo.ncalls;
				rwlockObjs.addAll(tinfo.objs);
				rwlockSites.addAll(tinfo.sites);
			}
		}
		nrwlockCallsPerSec /= durationSec;		
		
		float nsemCallsPerSec = 0;
		int nsemThreads = 0;
		HashSet<Object> semObjs = new HashSet<Object>();
		HashSet<StackTraceElement> semSites = new HashSet<StackTraceElement>();		
		for (ThreadInfo tinfo: this.semStats) {
			if (tinfo.ncalls > 0) {
				nsemThreads++;
				nsemCallsPerSec += tinfo.ncalls;
				semObjs.addAll(tinfo.objs);
				semSites.addAll(tinfo.sites);
			}
		}
		nsemCallsPerSec /= durationSec;
		
		float nflockCallsPerSec = 0;
		int nflockThreads = 0;
		HashSet<Object> flockObjs = new HashSet<Object>();
		HashSet<StackTraceElement> flockSites = new HashSet<StackTraceElement>();		
		for (ThreadInfo tinfo: this.flockStats) {
			if (tinfo.ncalls > 0) {
				nflockThreads++;
				nflockCallsPerSec += tinfo.ncalls;
				flockObjs.addAll(tinfo.objs);
				flockSites.addAll(tinfo.sites);
			}
		}
		nflockCallsPerSec /= durationSec;
		
		System.out.println("-----------Object.wait()------------");
		System.out.println("number of calls/sec = "+ nwaitCallsPerSec);
		System.out.println("number of threads = "+ nwaitThreads);
		System.out.println("number of objects = "+ waitObjs.size());
		System.out.println("number of call sites = "+ waitSites.size());
		
		System.out.println("-----------Object.notify()------------");
		System.out.println("number of calls/sec = "+ nnotifyCallsPerSec);
		System.out.println("number of threads = "+ nnotifyThreads);
		System.out.println("number of objects = "+ notifyObjs.size());
		System.out.println("number of call sites = "+ notifySites.size());
		
		System.out.println("-----------ReentrantReadWriteLock.lock()------------");
		System.out.println("number of calls/sec = "+ nrwlockCallsPerSec);
		System.out.println("number of threads = "+ nrwlockThreads);
		System.out.println("number of objects = "+ rwlockObjs.size());
		System.out.println("number of call sites = "+ rwlockSites.size());
		
		System.out.println("-----------Semaphore.acquire()------------");
		System.out.println("number of calls/sec = "+ nsemCallsPerSec);
		System.out.println("number of threads = "+ nsemThreads);
		System.out.println("number of objects = "+ semObjs.size());
		System.out.println("number of call sites = "+ semSites.size());
		
		System.out.println("-----------FileChannel.lock()------------");
		System.out.println("number of calls/sec = "+ nflockCallsPerSec);
		System.out.println("number of threads = "+ nflockThreads);
		System.out.println("number of objects = "+ flockObjs.size());
		System.out.println("number of call sites = "+ flockSites.size());
		
	}
}
