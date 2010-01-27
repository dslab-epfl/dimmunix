package dIV.core.staticAnalysis;

import soot.SootMethod;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.Stmt;

/**
 * Class for keeping information about a lock event; keeps the lock and type of
 * the event related to that lock (is it a acquire or release event)
 * 
 * @author pinar
 */
public class LockEvent {
	public SootMethod method;
	public Stmt stmt;
	public boolean isSyncMethod;

	public LockEvent(SootMethod method, Stmt stmt, boolean isSynchMethod) {
		this.method = method;
		this.stmt = stmt;
		this.isSyncMethod = isSynchMethod;
	}

	public boolean isAcquire(){
		return (this.stmt instanceof EnterMonitorStmt);
	}
	
	public void print() {
		System.out.println("--Lock Event--");
		System.out.println("----Method: " + this.method.getSignature());
		System.out.println("----Stmt: " + this.stmt.toString() + " line=" +this.stmt.getTag("LineNumberTag"));
	}
}