package dimmunix.analysis;

import java.lang.management.MonitorInfo;
import java.util.HashSet;
import java.util.List;

import soot.Unit;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.CallStack;
import dimmunix.Pair;
import dimmunix.Vector;

public class OuterCallStackAnalysis {
	public static final OuterCallStackAnalysis instance = new OuterCallStackAnalysis();
	
	public static final int MAX_CALL_STACK_DEPTH = 20;
	
	private OuterCallStackAnalysis() {		
	}
	
	public CallStack findOuterCallStack(int targetNestingLevel, StackTraceElement[] callStack, Vector<Pair<ExceptionalUnitGraph, Unit>> framesOuter, Vector<Pair<ExceptionalUnitGraph, Unit>> framesInner) throws Throwable {
		return this.findOuterCallStack(0, targetNestingLevel, 0, callStack, 0, framesOuter, framesInner);
	}
	
	private CallStack findOuterCallStack(int nestingLevel, int targetNestingLevel, int frameIndex, StackTraceElement[] callStack, int nExploredTotal, Vector<Pair<ExceptionalUnitGraph, Unit>> framesOuter, Vector<Pair<ExceptionalUnitGraph, Unit>> framesInner) throws Throwable {
		String className = callStack[frameIndex].getClassName();
		String methodName = callStack[frameIndex].getMethodName();
		int lineNumber = callStack[frameIndex].getLineNumber();
		
		Pair<ExceptionalUnitGraph, Unit> frame = Analysis.instance.findFrame(className, methodName, lineNumber);
		if (frame == null)
			throw new Exception("could not infer outer call stack");
		
		if (nExploredTotal == 0 && framesInner != null)
			framesInner.add(frame);
		Stmt waitStmt = null;
		if (nExploredTotal == 0) {
			Stmt syncStmt = this.findSyncStmtAtLine(frame.v1, lineNumber);
			if (syncStmt != null && Analysis.instance.isWait(syncStmt)) {
				waitStmt = syncStmt;
			}
		}
		return this.findOuterCallStackInFrame(waitStmt, nestingLevel, targetNestingLevel, frameIndex, callStack, frame.v1, frame.v2, new HashSet<Unit>(), nExploredTotal, framesOuter, framesInner);
	}
	
	CallStack findOuterCallStackInFrame(Stmt waitStmt, int nestingLevel, int targetNestingLevel, int frameIndex, StackTraceElement[] callStack, ExceptionalUnitGraph ug, Unit stmt, HashSet<Unit> explored, int nExploredTotal, Vector<Pair<ExceptionalUnitGraph, Unit>> framesOuter, Vector<Pair<ExceptionalUnitGraph, Unit>> framesInner) throws Throwable {
		if (explored.contains(stmt)) {
			return null;
		}
		explored.add(stmt);
		nExploredTotal++;
		
		if (stmt instanceof EnterMonitorStmt || stmt == ug.getHeads().get(0) && ug.getBody().getMethod().isSynchronized()) {
			nestingLevel++;
			if (waitStmt != null && stmt instanceof EnterMonitorStmt && Analysis.instance.mustAliasLockCall(ug, (EnterMonitorStmt)stmt, waitStmt)) {
				nestingLevel--;
			}
			if (nestingLevel == targetNestingLevel) {
				CallStack cs = new CallStack(MAX_CALL_STACK_DEPTH);
				LineNumberTag ltag = (LineNumberTag)stmt.getTag("LineNumberTag");
				if (ltag == null)
					throw new Exception("could not infer outer call stack");
				StackTraceElement f = callStack[frameIndex];
				cs.add(new StackTraceElement(f.getClassName(), f.getMethodName(), f.getFileName(), ltag.getLineNumber()));
				for (int d = 1; frameIndex+ d < callStack.length && d < MAX_CALL_STACK_DEPTH; d++) {
					cs.add(callStack[frameIndex+ d]);
				}
				if (framesOuter != null) {
					framesOuter.add(new Pair<ExceptionalUnitGraph, Unit>(ug, stmt));					
				}
				return cs;
			}
		}
		if (stmt instanceof ExitMonitorStmt) {
			nestingLevel--;
		}
		
		List<Unit> preds = ug.getPredsOf(stmt);		
		if (preds.isEmpty()) {			
			if (frameIndex < callStack.length- 1) {
				return findOuterCallStack(nestingLevel, targetNestingLevel, frameIndex+ 1, callStack, nExploredTotal, framesOuter, framesInner);
			}
			else {
				throw new Exception("could not infer outer call stack");
			}
		}
		for (Unit sPrev: preds) {
			CallStack p = this.findOuterCallStackInFrame(waitStmt, nestingLevel, targetNestingLevel, frameIndex, callStack, ug, sPrev, explored, nExploredTotal, framesOuter, framesInner);
			if (p != null)
				return p;
		}
		throw new Exception("could not infer outer call stack");
	}
	
/*	public int findLineNumberInCFG(StackTraceElement frame) throws Throwable {
		String className = frame.getClassName();
		String methodName = frame.getMethodName();
		int line = frame.getLineNumber();
		
		SootClass cl = Scene.v().loadClass(className, SootClass.BODIES);
		if (cl.isPhantomClass())
			throw new Exception("could not infer outer call stack in "+ cl);
		for (SootMethod m: cl.getMethods()) {			
			if (m.isConcrete() && m.getName().equals(methodName)) {
				Body body = m.retrieveActiveBody();
				for (Unit u: body.getUnits()) {
					Stmt s = (Stmt)u;
					LineNumberTag ltag = (LineNumberTag)s.getTag("LineNumberTag");
					if (ltag != null && ltag.getLineNumber() == line) {
						ExceptionalUnitGraph ug = new ExceptionalUnitGraph(body);
						List<Unit> preds = ug.getPredsOf(s);
						if (preds.isEmpty() && m.isSynchronized()) {
							return line;
						}
						else {							
							boolean foundMonenter = false;
							for (Unit uprev: preds) {
								if (uprev instanceof EnterMonitorStmt) {
									foundMonenter = true;
									ltag = (LineNumberTag)uprev.getTag("LineNumberTag");
									if (ltag != null) {
										return ltag.getLineNumber();
									}
								}
							}
							if (!foundMonenter) {
								if (s instanceof EnterMonitorStmt) {
									return line;
								}
								if (Analysis.instance.isExplicitLock(s)) {
									return line;
								}
							}							
						}
					}
				}
			}
		}
		
		throw new Exception("could not find line number for "+ frame);
	}*/
	
	private Stmt findSyncStmtAtLine(ExceptionalUnitGraph ug, int line) {
		for (Unit u: ug.getBody().getUnits()) {
			Stmt s = (Stmt)u;
			if (Analysis.instance.isLock(s) || Analysis.instance.isExplicitLock(s) || Analysis.instance.isWait(s)) {
				LineNumberTag ltag = (LineNumberTag)s.getTag("LineNumberTag");
				if (ltag != null && ltag.getLineNumber() == line) {
					return s; 
				}
			}
		}
		
		return null;
	}
	
	public int findIndex(MonitorInfo[] lockStack, int id) {
		for (int k = lockStack.length- 1; k >= 0; k--) {
			if (lockStack[k].getIdentityHashCode() == id)
				return k+ 1;
		}
		
		return -1;
	}
}
