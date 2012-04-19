package dimmunix.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.Util;
import dimmunix.Vector;
import dimmunix.deadlock.DimmunixDeadlock;
import dimmunix.deadlock.Signature;

public class SkipAvoidanceAnalysis {
	
	public static final SkipAvoidanceAnalysis instance = new SkipAvoidanceAnalysis();
	
	//lines where to skip avoidance
	public HashSet<SkipAvoidance> positionsToSkipAvoidance = new HashSet<SkipAvoidance>();
	
	public void findPositionsToSkipAvoidance() {
//		System.out.println("finding positions to skip deadlock avoidance");
		
		for (Signature sig: DimmunixDeadlock.instance.history.historyQueue) {
			for (int i = 0; i < sig.size(); i++) {
				StackTraceElement outerPos = sig.positions.get(i).value.callStack.get(0);
				StackTraceElement innerPos = sig.innerPositions.get(i).callStack.get(0);
				
//				System.out.println("outer pos = "+ outerPos);
//				System.out.println("inner pos = "+ innerPos);
				
				for (StackTraceElement branchPos: this.getPositionsToSkip(sig.id, i, outerPos, innerPos, sig.innerPositions.get(i).callStack)) {
					this.positionsToSkipAvoidance.add(new SkipAvoidance(branchPos, sig.id, i, true));						
				}							
			}
		}
	}
	
	private void savePositionsToSkipAvoidance() {
//		System.out.println("saving positions to skip deadlock avoidance");

		try {
			PrintWriter pw = new PrintWriter("skip_avoidance");
			for (SkipAvoidance p : this.positionsToSkipAvoidance) {
				pw.write(p.toString() + "\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	private void findReachableMethods(Stmt s, ExceptionalUnitGraph ug, SootMethod mTarget, HashSet<Stmt> explored, HashSet<SootMethod> reachableMethods) {
		if (explored.contains(s))
			return;
		explored.add(s);
		
		if (s.containsInvokeExpr()) {
			SootMethod m = null;
			try {
				m = s.getInvokeExpr().getMethod();
			}
			catch (Throwable ex) {				
			}
			if (m != null) {
				for (Body b: Analysis.instance.findBodies(m)) {
					this.findReachableMethods_(b, mTarget, new HashSet<SootMethod>(), new HashMap<SootMethod, Boolean>(), reachableMethods);
				}
			}
		}
		
		for (Unit unext: ug.getUnexceptionalSuccsOf(s)) {
			this.findReachableMethods((Stmt)unext, ug, mTarget, explored, reachableMethods);
		}
	}
	
	private boolean findReachableMethods_(Body body, SootMethod mTarget, HashSet<SootMethod> exploring, HashMap<SootMethod, Boolean> reachabilityCache, HashSet<SootMethod> reachableMethods) {
		if (body.getMethod() == mTarget) {
			return true;
		}
		
		Boolean resultCache = reachabilityCache.get(body.getMethod());
		if (resultCache != null) {
			return resultCache;
		}
		
		if (exploring.contains(body.getMethod()))
			return false;
		exploring.add(body.getMethod());
		
		boolean result = false;
		for (Unit u: body.getUnits()) {
			Stmt s = (Stmt)u;			
			if (s.containsInvokeExpr()) {
				SootMethod m = null;
				try {
					m = s.getInvokeExpr().getMethod();
				}
				catch (Throwable ex) {				
				}
				if (m != null) {
					for (Body b: Analysis.instance.findBodies(m)) {
						boolean r = this.findReachableMethods_(b, mTarget, exploring, reachabilityCache, reachableMethods);
						result = result || r;
					}
				}
			}			
		}
		
		if (result) {
			reachableMethods.add(body.getMethod());
		}
		
		reachabilityCache.put(body.getMethod(), result);
		exploring.remove(body.getMethod());
		
		return result;
	}	
	
/*	private HashSet<SootMethod> getCallStackMethods(Vector<StackTraceElement> callStack) {
		HashSet<SootMethod> meths = new HashSet<SootMethod>();
		
		for (StackTraceElement f: callStack) {
			SootMethod m = Analysis.instance.getMethod(f);
			if (m != null) {
				meths.add(m);
			}
		}
		
		return meths;
	}*/
	
	private Vector<StackTraceElement> getPositionsToSkip(int sigId, int index, StackTraceElement outerLockPos, StackTraceElement innerLockPos, Vector<StackTraceElement> innerStack) {
		Stmt outerLock = Analysis.instance.getStatement(outerLockPos);
		Stmt innerLock = Analysis.instance.getStatement(innerLockPos);
		ExceptionalUnitGraph ugOuter = Analysis.instance.getUnitGraph(outerLockPos);
		ExceptionalUnitGraph ugInner = Analysis.instance.getUnitGraph(innerLockPos);
		
		if (ugOuter == null || ugInner == null) {
			return new Vector<StackTraceElement>();
		}
		
		SootMethod mStart = ugOuter.getBody().getMethod();
		SootMethod mTarget = ugInner.getBody().getMethod();
		
		Vector<StackTraceElement> posCriticalBranches = new Vector<StackTraceElement>();
//		HashSet<SootMethod> innerMethods = this.getCallStackMethods(innerStack);
				
		if (!Analysis.instance.isInCycle(ugInner, innerLock)) {
			this.positionsToSkipAvoidance.add(new SkipAvoidance(innerLockPos, sigId, index, false));
		}
		
		if (mStart == mTarget) {
			this.isReachableIntraProc(ugInner, outerLock, innerLock, new HashSet<Stmt>(), new HashMap<Stmt, Boolean>(), posCriticalBranches);			
		}
		else {
			HashSet<SootMethod> reachableMethods = new HashSet<SootMethod>();

			this.findReachableMethods(outerLock, ugOuter, mTarget, new HashSet<Stmt>(), reachableMethods);
			
			this.isReachableInterProc(ugOuter, outerLock, mTarget, reachableMethods, new HashSet<Stmt>(), new HashMap<Stmt, Boolean>(), posCriticalBranches);
			
/*			for (SootMethod m: innerMethods) {
				try {
					ExceptionalUnitGraph ug = new ExceptionalUnitGraph(m.getActiveBody());
					this.isReachableInterProc(ug, (Stmt)ug.getHeads().get(0), mTarget, reachableMethods, new HashSet<Stmt>(), new HashMap<Stmt, Boolean>(), posCriticalBranches);									
				}
				catch (Throwable ex) {
					System.out.println("could not do reachability analysis in method "+ m);
				}
			}*/
			
			this.isReachableIntraProc(ugInner, (Stmt)ugInner.getHeads().get(0), innerLock, new HashSet<Stmt>(), new HashMap<Stmt, Boolean>(), posCriticalBranches);			
		}
		
		return posCriticalBranches;
	}
	
	private boolean isReachableInterProc(ExceptionalUnitGraph ug, Stmt s, SootMethod mTarget, HashSet<SootMethod> reachableMethods, HashSet<Stmt> exploring, HashMap<Stmt, Boolean> reachabilityCache, Vector<StackTraceElement> posCriticalBranches) {
		Boolean rCache = reachabilityCache.get(s);
		if (rCache != null)
			return rCache;
		
		if (exploring.contains(s))
			return false;
		exploring.add(s);
		
		if (s.containsInvokeExpr()) {
			SootMethod m = null;
			try {
				m = s.getInvokeExpr().getMethod();
			}
			catch (Throwable ex) {				
			}
			if (m != null) {
				for (Body b: Analysis.instance.findBodies(m)) {
					if (b.getMethod() == mTarget || reachableMethods.contains(b.getMethod())) {
						return true;
					}
				}
			}
		}			
		
		boolean result = false;
		
		List<Unit> succs = ug.getUnexceptionalSuccsOf(s); 
		Vector<Stmt> criticalBranches = new Vector<Stmt>();		
		for (Unit unext: succs) {
			Stmt snext = (Stmt)unext;
			boolean r = this.isReachableInterProc(ug, snext, mTarget, reachableMethods, exploring, reachabilityCache, posCriticalBranches);
			result = result || r;
			if (!r) {
				criticalBranches.add(snext);
			}
		}
		
		if (result) {
			for (Stmt br: criticalBranches) {
				if (!Analysis.instance.isInCycle(ug, br)) {					
					posCriticalBranches.add(Util.getPosition(ug.getBody().getMethod(), br));									
				}
			}
		}
		
		reachabilityCache.put(s, result);
		exploring.remove(s);
		
		return result;
	}
	
	private boolean isReachableIntraProc(ExceptionalUnitGraph ug, Stmt s, Stmt target, HashSet<Stmt> exploring, HashMap<Stmt, Boolean> reachabilityCache, Vector<StackTraceElement> posCriticalBranches) {
		if (s == target)
			return true;
		
		Boolean rCache = reachabilityCache.get(s);
		if (rCache != null)
			return rCache;
		
		if (exploring.contains(s))
			return false;
		exploring.add(s);
		
		boolean result = false;
		
		List<Unit> succs = ug.getUnexceptionalSuccsOf(s); 
		Vector<Stmt> criticalBranches = new Vector<Stmt>();		
		for (Unit unext: succs) {
			Stmt snext = (Stmt)unext;
			boolean r = this.isReachableIntraProc(ug, snext, target, exploring, reachabilityCache, posCriticalBranches);
			result = result || r;
			if (!r) {
				criticalBranches.add(snext);
			}
		}
		
		if (result) {
			for (Stmt br: criticalBranches) {
				if (!Analysis.instance.isInCycle(ug, br)) {					
					posCriticalBranches.add(Util.getPosition(ug.getBody().getMethod(), br));
				}
			}
		}
		
		reachabilityCache.put(s, result);
		exploring.remove(s);
		
		return result;
	}	

	public void run() {
		this.findPositionsToSkipAvoidance();
	}
	
	public void init() {
		this.loadPositionsToSkipAvoidance();
	}
	
	private void loadPositionsToSkipAvoidance() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("skip_avoidance"));
			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer lineTok = new StringTokenizer(line, " ");
				StackTraceElement pos = Util.parsePosition(lineTok.nextToken());
				int sigId = Integer.parseInt(lineTok.nextToken());
				int index = Integer.parseInt(lineTok.nextToken());
				boolean before = Boolean.parseBoolean(lineTok.nextToken());
				this.positionsToSkipAvoidance.add(new SkipAvoidance(pos, sigId, index, before));
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void saveResults() {
		this.savePositionsToSkipAvoidance();
	}
}
