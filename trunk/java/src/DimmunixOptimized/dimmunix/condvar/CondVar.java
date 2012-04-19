package dimmunix.condvar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class CondVar {
	public final int id;	
	public final Object obj;
	
	private HashSet<StackTraceElement> waitPositions = new HashSet<StackTraceElement>();
	private HashSet<StackTraceElement> lockBeforeNotifyPositions = new HashSet<StackTraceElement>();
	
	private ConcurrentSkipListSet<Long> notifiers = new ConcurrentSkipListSet<Long>();
	
	private StackTraceElement lastLockPos = null;
	
	public StackTraceElement getLastLockPos() {
		return lastLockPos;
	}

	public void setLastLockPos(StackTraceElement lastLockPos) {
		this.lastLockPos = lastLockPos;
	}

	public boolean addWaitPosition(StackTraceElement pos) {
		return this.waitPositions.add(pos);
	}
	
	public Set<StackTraceElement> getWaitPositions() {		
		return Collections.unmodifiableSet(this.waitPositions);
	}
	
	public boolean addLockBeforeNotifyPosition(StackTraceElement pos) {
		return this.lockBeforeNotifyPositions.add(pos);
	}
	
	public Set<StackTraceElement> getLockBeforeNotifyPositions() {
		return Collections.unmodifiableSet(this.lockBeforeNotifyPositions);
	}
	
	public CondVar(int id, Object obj) {
		this.id = id;
		this.obj = obj;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof CondVar) {
			CondVar c = (CondVar)obj;
			return this.obj == c.obj;
		}
		return false;
	}
	
	public int hashCode() {
		return id;
	}
	
	public boolean addNotifier(long tid) {
		return this.notifiers.add(tid);
	}
	
	public boolean isNotifier(long tid) {
		return this.notifiers.contains(tid);
	}
	
	public String toString() {
		return "cond_var_"+ this.id;
	}
}
