package dimmunix.init;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.aspectj.weaver.loadtime.ExecutionPositions;
import org.aspectj.weaver.loadtime.SyncPositions;

import dimmunix.CallStack;
import dimmunix.Vector;
import dimmunix.analysis.OuterCallStackAnalysis;

public class DeadlockDetector {
	
	private class BlockedInsideInitInfo {
		public final StackTraceElement position;
		public final LockInfo lockInfo;
		public final String clsName;
		public final Long threadId;
		
		public BlockedInsideInitInfo(StackTraceElement position, LockInfo lockInfo, String clsName, Long threadId) {
			this.position = position;
			this.lockInfo = lockInfo;
			this.clsName = clsName;
			this.threadId = threadId;
		}		
	}
	
	private class BlockedBeforeInitInfo {
		public final StackTraceElement[] callStack;
		public final MonitorInfo[] lockStack;
		public final String clsName;
		public final Long threadId;
		
		public BlockedBeforeInitInfo(StackTraceElement[] callStack, MonitorInfo[] lockStack, String clsName, Long threadId) {
			this.callStack = callStack;
			this.lockStack = lockStack;
			this.clsName = clsName;
			this.threadId = threadId;
		}		
	}
	
	public static final DeadlockDetector instance = new DeadlockDetector();
		
	private DeadlockDetector() {
	}
	
	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	public void run() {
		System.out.println("checking for initialization deadlocks");
		
		ThreadInfo[] threadsInfo = this.threadMXBean.dumpAllThreads(true, true);

		Vector<BlockedInsideInitInfo> blockedInsideInitInfos = new Vector<DeadlockDetector.BlockedInsideInitInfo>();
		Vector<BlockedBeforeInitInfo> blockedBeforeInitInfos = new Vector<DeadlockDetector.BlockedBeforeInitInfo>();

		for (ThreadInfo tinfo: threadsInfo) {
			Thread.State state = tinfo.getThreadState();
			LockInfo linfo = tinfo.getLockInfo();
			long tid = tinfo.getThreadId();
			StackTraceElement[] callStack = tinfo.getStackTrace();			
			if (callStack.length == 0) {
				continue;
			}
			StackTraceElement pos = callStack[0];
						
			if (linfo != null && pos.getMethodName().equals("<clinit>")) {
				blockedInsideInitInfos.add(new BlockedInsideInitInfo(pos, linfo, pos.getClassName(), tid));
			}
			String clsInit;
			if (state == Thread.State.RUNNABLE && (clsInit = StaticInitAnalysis.instance.isInitPosition(pos)) != null) {
				blockedBeforeInitInfos.add(new BlockedBeforeInitInfo(callStack, tinfo.getLockedMonitors(), clsInit, tid));
			}
		}
		
		for (BlockedBeforeInitInfo beforeInit: blockedBeforeInitInfos) {
			for (BlockedInsideInitInfo insideInit: blockedInsideInitInfos) {
				this.checkForDeadlocks(beforeInit, insideInit);
			}
		}
	}
	
	private void checkForDeadlocks(BlockedBeforeInitInfo beforeInit, BlockedInsideInitInfo insideInit) {
		if (beforeInit.clsName.equals(insideInit.clsName)) {
			for (MonitorInfo lheld: beforeInit.lockStack) {
				if (lheld.getIdentityHashCode() == insideInit.lockInfo.getIdentityHashCode()) {
					//deadlock
					System.out.println("initialization deadlock found !");
					
					int lindex = OuterCallStackAnalysis.instance.findIndex(beforeInit.lockStack, insideInit.lockInfo.getIdentityHashCode());
					try {
						CallStack outerCallStack = OuterCallStackAnalysis.instance.findOuterCallStack(lindex, beforeInit.callStack, null, null);

						StackTraceElement lockPos = outerCallStack.get(0);
						Signature sig = new Signature(lockPos, insideInit.clsName);

						History.instance.addSignature(sig);
						
						ExecutionPositions.instance.addPosition(lockPos);
						SyncPositions.add(lockPos);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
