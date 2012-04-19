package dimmunix.condvar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.loadtime.ExecutionPositions;

import dimmunix.Util;
import dimmunix.analysis.CondVarAnalysis;


public class DimmunixCondVar {
	
	public static DimmunixCondVar instance = new DimmunixCondVar();
	
	public static final int CALL_STACK_DEPTH = 5;
	public static final int CALL_STACK_OFFSET = 4;
	
	private HashSet<StackTraceElement> lockBeforeNotifyPositions = new HashSet<StackTraceElement>();
	private HashSet<StackTraceElement> newLockBeforeNotifyPositions = new HashSet<StackTraceElement>();
	
	private ConcurrentHashMap<Integer, CondVar> condVars = new ConcurrentHashMap<Integer, CondVar>();
	private ConcurrentHashMap<Long, ThreadInfo> threads = new ConcurrentHashMap<Long, ThreadInfo>();
	
	public final boolean inlineMatching = true;
	
	private DimmunixCondVar() {
	}
	
	public CondVar getCondVar(int id, Object x) {
		Integer idX = id;
		CondVar c = this.condVars.get(idX);
		if (c == null) {
			synchronized (this.condVars) {
				c = this.condVars.get(idX);
				if (c == null) {
					c = new CondVar(id, x);
					this.condVars.put(idX, c);
				}
			}
		}
		return c;
	}
	
	public CondVar getCondVar(Object x) {
		return this.getCondVar(System.identityHashCode(x), x);
	}
	
	public ThreadInfo getThreadInfo(long tid) {
		Long tid_ = tid;
		ThreadInfo tinfo = this.threads.get(tid_);
		if (tinfo == null) {
			synchronized (this.threads) {
				tinfo = this.threads.get(tid_);
				if (tinfo == null) {
					tinfo = new ThreadInfo(tid);
					this.threads.put(tid_, tinfo);
				}
			}
		}		
		return tinfo;
	}
	
	public void beforeLock(Object x) {
		Thread t = Thread.currentThread();
		CondVar c = this.getCondVar(x);
		ThreadInfo tinfo = this.getThreadInfo(t.getId());
		
		StackTraceElement pos = null;
		if (this.inlineMatching) {
			pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
		}
		else {
			tinfo.getCallStack();
			pos = tinfo.callStack.get(0);						
		}
		
		tinfo.setLastLockReqPos(pos);
		
		for (Signature sig: History.instance.getSignatures()) {
			if (sig.syncBeforeWaitPos.equals(pos)) {
				sig.addYielder(tinfo);
				tinfo.startYield(sig);				
				return;
			}
		}
		
		if (CondVarAnalysis.instance.isLockBeforeNotifyPosition(pos)) {		
			if (this.lockBeforeNotifyPositions.contains(pos)) {
				for (Signature sig: History.instance.getSignatures()) {
					if (sig.getLockBeforeNotifyPositions().contains(pos)) {
						sig.wakeUpYielders();
					}			
				}							
			}
			else {
				//new lockbeforenotify pos
				synchronized (newLockBeforeNotifyPositions) {
					this.newLockBeforeNotifyPositions.add(pos);				
				}
				
				for (Signature sig: History.instance.getSignatures()) {
					sig.wakeUpYielders();
				}				
			}
		}		
	}
	
	public void afterLock(Object x) {
		Thread t = Thread.currentThread();
		CondVar c = this.getCondVar(x);
		ThreadInfo tinfo = this.getThreadInfo(t.getId());		
		
		c.setLastLockPos(tinfo.getLastLockReqPos());		
	}
	
	public void beforeUnlock(Object x) {		
		Thread t = Thread.currentThread();
		CondVar c = this.getCondVar(x);
		ThreadInfo tinfo = this.getThreadInfo(t.getId());

		if (tinfo.isYielding()) {
			StackTraceElement yieldPos = c.getLastLockPos();
			
			for (Signature sig: History.instance.getSignatures()) {
				if (sig.syncBeforeWaitPos.equals(yieldPos)) {
					sig.removeYielder(tinfo);							
				}
			}
			
			tinfo.doneYield();
		}
	}
	
	public void beforeWait(Object x, long timeout) {
		Thread t = Thread.currentThread();
		CondVar c = this.getCondVar(x);
		ThreadInfo tinfo = this.getThreadInfo(t.getId());

		StackTraceElement pos = null;
		if (this.inlineMatching) {
			pos = ExecutionPositions.instance.getCurrentPosition(t.getId());
		}
		else {
			tinfo.getCallStack();
			pos = tinfo.callStack.get(0);						
		}
		
		tinfo.setLastWaitTimeout(timeout);
		
		if (pos != null) {//pos == null in the first run						
			c.addWaitPosition(pos);
			
			for (Signature sig: History.instance.getSignatures()) {
				if (sig.waitPos.equals(pos)) {
					sig.removeYielder(tinfo);
				}
			}
			
			tinfo.doneYield();
		}
	}
	
	public void afterWait(Object obj, long timeout) {
	}
	
	public void beforeNotify(Object x) {
		Thread t = Thread.currentThread();
		CondVar c = this.getCondVar(x);		
		
		StackTraceElement pos = c.getLastLockPos();

		c.addNotifier(t.getId());
		
		if (pos != null) {
			//pos == null in the first run
			c.addLockBeforeNotifyPosition(pos);				
		}
	}
	
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("lock_before_notify_positions"));			
			String line;
			while ((line = br.readLine()) != null) {
				this.lockBeforeNotifyPositions.add(Util.parsePosition(line));
			}			
			br.close();			
		} catch (Exception e) {
		}
		
		History.instance.init();
	}
	
	public void shutDown() {
		BlockedNotificationDetector.instance.run();
		
		//update locks before notifies for the signatures
		for (CondVar c: this.condVars.values()) {
			for (StackTraceElement wpos: c.getWaitPositions()) {
				History.instance.addLockBeforeNotifyPositions(wpos, c.getLockBeforeNotifyPositions());
			}
		}
		
		try {
			this.lockBeforeNotifyPositions.addAll(this.newLockBeforeNotifyPositions);
			PrintWriter pw = new PrintWriter("lock_before_notify_positions");			
			for (StackTraceElement p: this.lockBeforeNotifyPositions) {
				pw.println(p);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		History.instance.save();
	}
}
