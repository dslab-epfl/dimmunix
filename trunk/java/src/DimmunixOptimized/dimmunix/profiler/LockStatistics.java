package dimmunix.profiler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.weaver.loadtime.SyncPositions;

import dimmunix.Configuration;
import dimmunix.SpecialStackTrace;
import dimmunix.Vector;

public class LockStatistics {

	public static LockStatistics instance = new LockStatistics();

	private long timeStart;
	private ConcurrentHashMap<Integer, LockInfo> locks;
	private ConcurrentHashMap<Vector<StackTraceElement>, LockPositionInfo> lockPositions;
	private HashSet<StackTraceElement> unlockPositions;
	private Vector<ThreadInfo> threads = new Vector<ThreadInfo>(3000);
	
	private boolean profileJustSyncsPerSec = false;
	private AtomicInteger nSyncs = new AtomicInteger(0);

	private ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
	
	private int offset = 3;
	private int depth = 10;
	
	private int nCommunixStacks = 100;
	
	private LockStatistics() {
		locks = new ConcurrentHashMap<Integer, LockInfo>();
		lockPositions = new ConcurrentHashMap<Vector<StackTraceElement>, LockPositionInfo>();
		unlockPositions = new HashSet<StackTraceElement>();

		timeStart = System.nanoTime();
		
		for (int i = 0; i < threads.capacity(); i++) {
			threads.add(new ThreadInfo());
		}
	}

	public void afterLock(Object o) {
		this.nSyncs.getAndIncrement();
		
		if (this.profileJustSyncsPerSec) {
			return;
		}
		
		Integer objId = System.identityHashCode(o);
		Vector<StackTraceElement> pos = null;
		
		pos = new Vector<StackTraceElement>();
		SpecialStackTrace.getStackTrace(pos, depth+ offset, offset);			
		
		LockPositionInfo pinfo = lockPositions.get(pos);
		if (pinfo == null) {
			pinfo = new LockPositionInfo(pos);
			LockPositionInfo pinfoOld = lockPositions.putIfAbsent(pos, pinfo);
			if (pinfoOld != null)
				pinfo = pinfoOld;
		}
		
		LockInfo linfo = locks.get(objId);
		if (linfo == null) {
			linfo = new LockInfo(objId);
			LockInfo linfoOld = locks.putIfAbsent(objId, linfo);
			if (linfoOld != null)
				linfo = linfoOld;
		} 
		linfo.n++;
		if (linfo.n == 1) {
			linfo.acqPos = pos.get(0);
			linfo.acqPosInfo = pinfo;
			linfo.tAcq = System.nanoTime();
		}
		else {
			return;
		}

		ThreadInfo t = threads.get((int)Thread.currentThread().getId());
		for (LockInfo lh: t.locksHeld) {
			lh.acqPosInfo.nested = true;
		}
		t.locksHeld.add(linfo);
		
		pinfo.lockObjects.add(linfo);
		pinfo.lockThreads.add(Thread.currentThread());
		
		pinfo.nacquired.getAndIncrement();
	}

	public void beforeUnlock(Object l) {
		if (this.profileJustSyncsPerSec) {
			return;
		}
		
		Integer objId = System.identityHashCode(l);
		
		LockInfo linfo = this.locks.get(objId);
		
		linfo.n--;
		
		if (linfo.n > 0)
			return;
		
		ThreadInfo t = threads.get((int)Thread.currentThread().getId());
		t.locksHeld.remove(linfo);
		
		StackTraceElement pos = Thread.currentThread().getStackTrace()[offset];
		if (SyncPositions.syncPositions.contains(linfo.acqPos)) {
			this.unlockPositions.add(pos);
		}
		
		linfo.tSync += System.nanoTime()- linfo.tAcq;
	}
	
	public void beforeWait(Object obj) {
		ThreadInfo t = threads.get((int)Thread.currentThread().getId());
		for (LockInfo lh: t.locksHeld) {
			lh.acqPosInfo.nested = true;
		}
	}

	public void beforeNotify(Object obj) {
		ThreadInfo t = threads.get((int)Thread.currentThread().getId());
		for (LockInfo lh: t.locksHeld) {
			lh.acqPosInfo.nested = true;
		}
	}
	
	public void printStatistics() {
		double execTimeSec = ((double) (System.nanoTime() - timeStart)) / 1000 / 1000 / 1000;
		System.out.println("EXECUTION TIME = " + execTimeSec + " seconds");

		System.out.println("THREADS = " + threadBean.getPeakThreadCount());
		System.err.println("LOCK OPS/ SEC = " + (this.nSyncs.get() / execTimeSec));
		System.err.println("LOCK OPS = " + this.nSyncs.get());
		System.out.println("LOCK OBJECTS = " + locks.keySet().size());
		System.out.println("CALL STACKS = " + lockPositions.keySet().size());

		Vector<LockPositionInfo> lockPosInfos = new Vector<LockPositionInfo>(lockPositions.values());
		
		if (!Configuration.instance.communixEnabled) {
			//remove unnested positions 
			for (int i = 0; i < lockPosInfos.size(); i++) {
				if (!lockPosInfos.get(i).nested) {
					lockPosInfos.remove(i);
					i--;
				}
			}
			
			double nSyncsNested = 0;
			for (LockPositionInfo pinfo: lockPosInfos) {
				for (LockInfo linfo: pinfo.lockObjects) {
					pinfo.tSync += linfo.tSync;
				}
				nSyncsNested += pinfo.nacquired.get();
			}
			System.err.println("LOCK OPS NESTED [%] = " + nSyncsNested* 100/ this.nSyncs.get());
			
			System.out.println("sorting the stacks...");
			for (int i = 0; i < lockPosInfos.size() - 1 && i < 20; i++) {
				for (int j = i + 1; j < lockPosInfos.size(); j++) {
					if (lockPosInfos.get(j).nacquired.get() > lockPosInfos.get(i).nacquired.get()) {
						LockPositionInfo aux = lockPosInfos.get(i);
						lockPosInfos.set(i, lockPosInfos.get(j));
						lockPosInfos.set(j, aux);
					}
				}
			}

			try {
				PrintWriter pw1 = new PrintWriter("lock_positions");
				PrintWriter pw2 = new PrintWriter("lock_stacks");
				
				double nsyncsTop20 = 0;
				HashSet<LockInfo> locksTop20 = new HashSet<LockInfo>();			
				for (int i = 0; i < 20 && i < lockPosInfos.size(); i++) {
					LockPositionInfo pinfo = lockPosInfos.get(i);
					nsyncsTop20 += pinfo.nacquired.get();
					locksTop20.addAll(pinfo.lockObjects);
				}
				
				System.out.println("TOP 20 BUSIEST STACKS, covering "+ (nsyncsTop20* 100/ nSyncsNested)+ "% of the nested syncs with "+ locksTop20.size()+ " locks");
				for (int i = 0; i < 20 && i < lockPosInfos.size(); i++) {
					LockPositionInfo pinfo = lockPosInfos.get(i);
					double syncsPercentage = pinfo.nacquired.get()* (double)100/ this.nSyncs.get(); 
					System.out.println(syncsPercentage + "% syncs performed by "+ pinfo.lockThreads.size()+ " threads on " +  pinfo.lockObjects.size()+ " objects at \n" + pinfo);
					pw1.write(pinfo.pos.get(0)+ "\n");
					pw2.write(pinfo+ "\n");				
				}
				
				pw1.close();
				pw2.close();
				
				PrintWriter pw3 = new PrintWriter("unlock_positions");
				System.out.println("UNLOCK POSITIONS:");
				for (StackTraceElement p : unlockPositions) {
					System.out.println(p);
					pw3.write(p+ "\n");
				}		
				pw3.close();			
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}					
		}
		else {
			try {
				System.out.println("finding the top "+ this.nCommunixStacks+ " busiest stacks");
				
				for (int i = 0; i < lockPosInfos.size() - 1 && i < this.nCommunixStacks; i++) {
					for (int j = i + 1; j < lockPosInfos.size(); j++) {
						if (lockPosInfos.get(j).nacquired.get() > lockPosInfos.get(i).nacquired.get()) {
							LockPositionInfo aux = lockPosInfos.get(i);
							lockPosInfos.set(i, lockPosInfos.get(j));
							lockPosInfos.set(j, aux);
						}
					}
				}
				
				System.out.println("saving the stacks");
				
				PrintWriter pw = new PrintWriter("communix_stacks");
				
				for (int i = 0; i < lockPosInfos.size() && i < this.nCommunixStacks; i++) {
					pw.println(lockPosInfos.get(i).getCommunixCallStack());
				}
				
				pw.close();
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
