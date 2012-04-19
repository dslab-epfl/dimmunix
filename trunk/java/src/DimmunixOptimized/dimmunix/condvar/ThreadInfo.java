package dimmunix.condvar;

import dimmunix.CallStack;
import dimmunix.SpecialStackTrace;

public class ThreadInfo implements Comparable<ThreadInfo> {
	public final CallStack callStack = new CallStack(DimmunixCondVar.CALL_STACK_DEPTH);
	
	public final long id;
	
	private long lastWaitTimeout = 0;
	
	private StackTraceElement lastLockReqPos = null;
	
	private final Object yieldLock = new Object();
	private final Object yieldLock2 = new Object();
	private Signature yieldInfo = null;
	private Signature yieldInfo2 = null;
	
	public StackTraceElement getLastLockReqPos() {
		return this.lastLockReqPos;
	}

	public void setLastLockReqPos(StackTraceElement lastLockReqPos) {
		this.lastLockReqPos = lastLockReqPos;
	}

	public long getLastWaitTimeout() {
		return lastWaitTimeout;
	}

	public void setLastWaitTimeout(long lastWaitTimeout) {
		this.lastWaitTimeout = lastWaitTimeout;
	}

	public ThreadInfo(long id) {
		this.id = id;
	}
	
	public void getCallStack() {
		SpecialStackTrace.getStackTrace(this.callStack, DimmunixCondVar.CALL_STACK_DEPTH+ DimmunixCondVar.CALL_STACK_OFFSET, DimmunixCondVar.CALL_STACK_OFFSET);
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
	
	public String toString() {
		return "thread_"+ this.id;
	}

	public int compareTo(ThreadInfo tinfo) {
		return (int)this.id- (int)tinfo.id;
	}
	
	public void startYield(Signature sig) {
		synchronized (yieldLock) {
			this.yieldInfo = sig;
			
			try {
				this.yieldLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
	}
	
	public void doneYield() {
		synchronized (yieldLock) {
			this.yieldInfo = null;
		}
		synchronized (yieldLock2) {
			this.yieldInfo2 = null;
			yieldLock2.notifyAll();			
		}
	}
	
	public void resumeAndWaitForYieldToFinish(Signature sig) {
		boolean resumed = false;
		synchronized (yieldLock) {
			if (this.yieldInfo == sig) {
				this.yieldLock.notify();
				resumed = true;
				synchronized (yieldLock2) {
					this.yieldInfo2 = this.yieldInfo;
				}
			}			
		}
		
		if (!resumed)
			return;

		synchronized (yieldLock2) {
			if (this.yieldInfo2 == sig) {
				try {
					this.yieldLock2.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}					
			}
		}
	}
	
	public boolean isYielding() {
		return this.yieldInfo != null;
	}
}
