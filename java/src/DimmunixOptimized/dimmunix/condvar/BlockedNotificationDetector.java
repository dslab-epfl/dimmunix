package dimmunix.condvar;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import dimmunix.CallStack;
import dimmunix.Vector;
import dimmunix.analysis.OuterCallStackAnalysis;

public class BlockedNotificationDetector {
	
	private class WaitInfo {
		public final StackTraceElement position;
		public final MonitorInfo[] lockStack;
		public final StackTraceElement[] callStack;
		public final CondVar condVar;
		
		public WaitInfo(StackTraceElement position, MonitorInfo[] lockStack, StackTraceElement[] callStack, CondVar condVar) {
			this.position = position;
			this.lockStack = lockStack;
			this.callStack = callStack;
			this.condVar = condVar;
		}		
	}
	
	private class BlockedInfo {
		public final StackTraceElement position;
		public final LockInfo lockInfo;
		public final Long threadId;
		
		public BlockedInfo(StackTraceElement position, LockInfo lockInfo, Long threadId) {
			this.position = position;
			this.lockInfo = lockInfo;
			this.threadId = threadId;
		}		
	}
	
	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	public static BlockedNotificationDetector instance = new BlockedNotificationDetector();
	
	private BlockedNotificationDetector() {		
	}	
	
	private void checkForPotentialDeadlocks(WaitInfo winfo, BlockedInfo binfo) {
		int blockedLockId = binfo.lockInfo.getIdentityHashCode();			
		if (winfo.condVar.isNotifier(binfo.threadId)) {
			for (MonitorInfo minfo: winfo.lockStack) {
				if (minfo.getIdentityHashCode() == blockedLockId) {
					//potential deadlock
					System.out.println("new blocked notification found !");
					
					int lindex = OuterCallStackAnalysis.instance.findIndex(winfo.lockStack, blockedLockId);
					try {
						CallStack outerCallStack = OuterCallStackAnalysis.instance.findOuterCallStack(lindex, winfo.callStack, null, null);

						Signature sig = new Signature(winfo.position, outerCallStack.get(0), binfo.position);

						History.instance.addSignature(sig);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}		
	}
	
	public void run() {
//		System.out.println("checking for blocked notifications");
		
		ThreadInfo[] threadsInfo = this.threadMXBean.dumpAllThreads(true, true);
		
		Vector<WaitInfo> waitInfos = new Vector<WaitInfo>();
		Vector<BlockedInfo> blockedInfos = new Vector<BlockedInfo>();
		
		for (ThreadInfo tinfo: threadsInfo) {
			Thread.State state = tinfo.getThreadState();
			LockInfo linfo = tinfo.getLockInfo();
			long tid = tinfo.getThreadId();
						
			if (linfo != null) {
				int lid = linfo.getIdentityHashCode();
				dimmunix.condvar.ThreadInfo myTinfo = DimmunixCondVar.instance.getThreadInfo(tid);				
				MonitorInfo[] lstack = tinfo.getLockedMonitors();
				StackTraceElement[] callStack = tinfo.getStackTrace();	
				
				if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING && myTinfo.getLastWaitTimeout() == 0) {
					if (lstack.length > 0) {//if a waiter thread holds locks, it may be a blocked notification
						callStack = Arrays.copyOfRange(callStack, 2, callStack.length);
						waitInfos.add(new WaitInfo(callStack[0], lstack, callStack, DimmunixCondVar.instance.getCondVar(lid, null)));
					}
				}
				else if (state == Thread.State.BLOCKED) {
					blockedInfos.add(new BlockedInfo(callStack[0], linfo, tid));
				}
			}
		}
		
		for (WaitInfo winfo: waitInfos) {
			for (BlockedInfo binfo: blockedInfos) {
				this.checkForPotentialDeadlocks(winfo, binfo);
			}
		}
	}
}
