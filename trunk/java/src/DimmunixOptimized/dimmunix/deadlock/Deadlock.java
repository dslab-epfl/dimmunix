package dimmunix.deadlock;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.HashSet;

import soot.Unit;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.CallStack;
import dimmunix.Pair;
import dimmunix.Util;
import dimmunix.Vector;
import dimmunix.analysis.OuterCallStackAnalysis;

public class Deadlock {
	public Vector<ThreadInfo> threads = new Vector<ThreadInfo>();
	public Vector<LockInfo> locks = new Vector<LockInfo>();
	public Vector<LockNode> lockNodes = new Vector<LockNode>();
	public Vector<StackTraceElement[]> callStacks = new Vector<StackTraceElement[]>();
	public Vector<MonitorInfo[]> lockStacks = new Vector<MonitorInfo[]>();
	public Vector<LockInfo[]> ownableSyncStacks = new Vector<LockInfo[]>();
	
	boolean isYieldCycle;
	
	private Vector<Pair<ExceptionalUnitGraph, Unit>> framesOuter = new Vector<Pair<ExceptionalUnitGraph,Unit>>();
	private Vector<Pair<ExceptionalUnitGraph, Unit>> framesInner = new Vector<Pair<ExceptionalUnitGraph,Unit>>();
	
	private final int offsetSemAcquire = 9;
	
	public Deadlock(boolean isYieldCycle) {
		this.isYieldCycle = isYieldCycle;
	}
		
	public int size() {
		return threads.size();
	}
	
	public Signature getSignature() throws Throwable {
		Signature sig = new Signature(!this.isYieldCycle);
		
		for (int i = 0; i < this.size(); i++) {
			int prev = (i == 0)? (this.size()- 1): (i- 1);
			int lockIdPrev = (lockNodes.get(prev) != null)? lockNodes.get(prev).id: locks.get(prev).getIdentityHashCode();
			LockNode lprev = (lockNodes.get(prev) != null)? lockNodes.get(prev): DimmunixDeadlock.instance.rag.getLockNode(lockIdPrev);   
			int nestingLevelPrev = OuterCallStackAnalysis.instance.findIndex(lockStacks.get(i), lockIdPrev);//or +1
			
			StackTraceElement[] callStack = this.callStacks.get(i);
			if (callStack.length > this.offsetSemAcquire && callStack[this.offsetSemAcquire- 3].getClassName().equals("dimmunix.deadlock.BoundedSemaphore")) {
				callStack = Arrays.copyOfRange(callStack, this.offsetSemAcquire, callStack.length);
			}
			
			Position pOuter;
			if (nestingLevelPrev == -1) {
				if (lprev.acqPos != null) {
					//it is an ownable synchronizer, we monitor their call stacks
					pOuter = DimmunixDeadlock.instance.rag.getPosition(lprev.acqPos);
				}
				else {
					// it is a semaphore acquired in simple avoidance
					pOuter = DimmunixDeadlock.instance.rag.getPosition(lprev.semReqPos);
				}
				framesOuter.add(null);
				framesInner.add(null);
			}
			else {
				//it's a synchronized block, we find the outer call stack
				CallStack cs = OuterCallStackAnalysis.instance.findOuterCallStack(nestingLevelPrev, callStack, framesOuter, framesInner);
				pOuter = DimmunixDeadlock.instance.rag.getPosition(cs);
			}
			if (pOuter == null)
				throw new Exception("could not infer deadlock signature");
			sig.add(new SignaturePosition(pOuter, 1, sig, sig.size()));
			
			Vector<StackTraceElement> innerCallStack = new Vector<StackTraceElement>();
			for (int d = 0; d < DimmunixDeadlock.instance.maxCallStackDepth && d < callStack.length; d++) {
				if (d == 0) {
					StackTraceElement f = callStack[0];
					innerCallStack.add(new StackTraceElement(f.getClassName(), f.getMethodName(), f.getFileName(), f.getLineNumber()));
				}
				else
					innerCallStack.add(callStack[d]);
			}
			sig.addInner(new InnerPosition(innerCallStack));
		}
		
		return sig;
	}
	
	public HashSet<StackTraceElement> getSyncPositions(Signature sig) {
		HashSet<StackTraceElement> syncPositions = new HashSet<StackTraceElement>();

		int index = 0;
		for (SignaturePosition outerPos: sig.positions) {
			StackTraceElement syncPos = outerPos.value.callStack.get(0);
			Pair<ExceptionalUnitGraph, Unit> frameOuter = this.framesOuter.get(index);
			if (frameOuter != null) {
				if (frameOuter.v2 instanceof EnterMonitorStmt) {
					//it was a monenter
					syncPositions.add(syncPos);
					syncPositions.addAll(this.findMonitorExitPositions(frameOuter.v2, frameOuter.v1));					
				}
				else {
					//the start of sync method
					syncPositions.add(syncPos);
					Unit monEnter = this.findMonitorEnterAt(frameOuter.v1, syncPos.getLineNumber());
					if (monEnter != null) {
						syncPositions.addAll(this.findMonitorExitPositions(monEnter, frameOuter.v1));
					}
				}
			}
			
			index++;
		}
		
		index = 0;
		for (InnerPosition innerPos: sig.innerPositions) {
			StackTraceElement syncPos = innerPos.callStack.get(0);
			Pair<ExceptionalUnitGraph, Unit> frameInner = this.framesInner.get(index);
			if (frameInner != null) {
				if (frameInner.v2 instanceof EnterMonitorStmt) {
					//it was a monenter
					syncPositions.add(syncPos);
					syncPositions.addAll(this.findMonitorExitPositions(frameInner.v2, frameInner.v1));					
				}
				else {
					//the start of a sync method
					syncPositions.add(syncPos);					
					Unit monEnter = this.findMonitorEnterAt(frameInner.v1, syncPos.getLineNumber());
					if (monEnter != null) {						
						syncPositions.addAll(this.findMonitorExitPositions(monEnter, frameInner.v1));
					}
				}
			}
				
			index++;
		}
		
//		System.out.println("sync positions "+ syncPositions);
		
		return syncPositions;
	}
	
	HashSet<StackTraceElement> findMonitorExitPositions(Unit monEnter, ExceptionalUnitGraph ug) {
		HashSet<StackTraceElement> monExitPositions = new HashSet<StackTraceElement>();
		HashSet<Unit> monExits = new HashSet<Unit>();
		
		this.findMonitorExits(0, monEnter, ug, new HashSet<Unit>(), monExits);
		
		for (Unit e: monExits) {
			monExitPositions.add(Util.getPosition(ug.getBody().getMethod(), e));
		}
		
		return monExitPositions;
	}
	
	void findMonitorExits(int nesting, Unit s, ExceptionalUnitGraph ug, HashSet<Unit> explored, HashSet<Unit> monExits) {
		if (explored.contains(s))
			return;
		explored.add(s);
		
		if (s instanceof EnterMonitorStmt) {
			nesting++;
		}
		else if (s instanceof ExitMonitorStmt) {
			nesting--;
			if (nesting == 0) {
				monExits.add(s);
				return;
			}
		}
		
		for (Unit snext: ug.getSuccsOf(s)) {
			this.findMonitorExits(nesting, snext, ug, explored, monExits);
		}
	}
	
	Unit findMonitorEnterAt(ExceptionalUnitGraph ug, int line) {
		for (Unit u: ug.getBody().getUnits()) {
			if (u instanceof EnterMonitorStmt) {
				LineNumberTag ltag = (LineNumberTag)u.getTag("LineNumberTag");
				if (ltag != null && ltag.getLineNumber() == line)
					return u;
			}
		}
		
		return null;
	}
	
	public String toString() {
		String s = "";
		s += this.threads;
		s += "locks = "+ this.locks+ "\n";
		s += "lock stacks = \n";
		for (MonitorInfo[] stack: this.lockStacks) {
			s += Arrays.toString(stack)+ "\n";
		}
		return s;
	}
}
