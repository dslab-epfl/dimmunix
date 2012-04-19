package dimmunix.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.Configuration;
import dimmunix.Util;

public class NestedSyncBlockAnalysis {
	
	public static final NestedSyncBlockAnalysis instance = new NestedSyncBlockAnalysis();
	
	public HashSet<StackTraceElement> nestedSyncBlockPositions;
	
	HashMap<SootMethod, Boolean> isNestedCache;
	
	HashSet<SootMethod> methodsContainingLock;
	
	int nSyncBlocksNested;
	int nSyncBlocksUnnested;
	
	NestedSyncBlockAnalysis() {
		this.isNestedCache = new HashMap<SootMethod, Boolean>();
		
		this.nestedSyncBlockPositions = new HashSet<StackTraceElement>();
		this.nSyncBlocksNested = this.nSyncBlocksUnnested = 0;
		
		this.methodsContainingLock = new HashSet<SootMethod>();
	}
	
	public void run() {
		this.findNestestedSyncBlocks();		
	}
	
	void findNestestedSyncBlocks() {		
//		System.out.println("finding nested synchronized blocks/methods");
		
		double tStart = System.currentTimeMillis();
		
		for (SootClass cl: Analysis.instance.loadedClassesSoot) {
			if (Configuration.instance.skip(cl.getName()))
				continue;
//			System.out.println("----------analyzing class "+ cl+ " for nested sync blocks");			
			for (SootMethod m: cl.getMethods()) {
				if (m.isConcrete()) {
					try {
						Body b = m.retrieveActiveBody();
						ExceptionalUnitGraph ug = new ExceptionalUnitGraph(b);
						if (m.isSynchronized()) {
							this.syncMethodCheckNested(ug);
						}
						this.findNestedSyncBlocks(ug);
					}
					catch (Throwable ex) {
						System.out.println("could not analyze method "+ m);
					}
				}
			}
		}
		
//		System.out.println(this.nSyncBlocksNested+ " sync blocks/methods are nested");
//		System.out.println(this.nSyncBlocksUnnested+ " sync blocks/methods are not nested");
		double tEnd = System.currentTimeMillis();
		double durationSec = (tEnd- tStart)/ 1000;
//		System.out.println("it took "+ durationSec+ " seconds to find the nested sync blocks/methods");
	}
	
	private void findNestedSyncBlocks(ExceptionalUnitGraph ug) {
		for (Unit s: ug.getBody().getUnits()) {
			if (Analysis.instance.isLock((Stmt)s)) {		
				this.syncBlockCheckNested(ug, (EnterMonitorStmt)s);
			}
		}
	}
	
	private void syncBlockCheckNested(ExceptionalUnitGraph ug, EnterMonitorStmt enterMon) {
		SootMethod m = ug.getBody().getMethod();
		StackTraceElement monitorPosition = Util.getPosition(m, enterMon);
		
		if (monitorPosition == null)
			return;
		
//		System.out.println("checking if "+ monitorPosition+ " is nested");
		
		boolean isNested = this.syncBlockContainsLock(ug, m, enterMon, new HashSet<Stmt>());
		
		if (isNested) {
			this.nestedSyncBlockPositions.add(monitorPosition);
//			System.out.println(monitorPosition+ " is nested");
			this.nSyncBlocksNested++;
		}
		else {
//			System.out.println(monitorPosition+ " is not nested");
			this.nSyncBlocksUnnested++;
		}		
	}
	
	private boolean syncBlockContainsLock(ExceptionalUnitGraph ug, SootMethod m, Stmt sStart, HashSet<Stmt> explored) {
		if (explored.contains(sStart))
			return false;
		explored.add(sStart);
		
		for (Unit u: ug.getSuccsOf(sStart)) {
			Stmt s = (Stmt)u;

			if (Analysis.instance.isUnlock(s))
				return false;
			else if (Analysis.instance.isLock(s))
				return true;
			else if (s.containsInvokeExpr()) {				
				SootMethod mCalled = s.getInvokeExpr().getMethod();
				
				for (Body b: Analysis.instance.findBodies(mCalled)) {
					if (b.getMethod().isSynchronized())
						return true;
					if (this.methodContainsLock(b, new HashSet<SootMethod>()))
						return true;
				}
			}				
			
			if (this.syncBlockContainsLock(ug, m, s, explored))
				return true;
		}
		
		return false;
	}
	
	private void syncMethodCheckNested(ExceptionalUnitGraph ug) {
		SootMethod m = ug.getBody().getMethod();
		Stmt firstStmt = (Stmt)ug.getHeads().get(0);
		StackTraceElement positionFirst = Util.getPosition(m, firstStmt);
		Value thisRef = null; 
		
		if (positionFirst == null)
			return;
		
		if (firstStmt instanceof IdentityStmt) {			
			thisRef = ((IdentityStmt)firstStmt).getLeftOp();
		}
		
//		System.out.println("checking if "+ m+ " is nested");
		
		boolean isNested = Analysis.instance.containsLock(ug.getBody());

		if (!isNested) {
			for (Unit u: ug.getBody().getUnits()) {
				Stmt s = (Stmt)u;			
			
				if (!s.containsInvokeExpr())
					continue;
				
				SootMethod mCalled = s.getInvokeExpr().getMethod();
				
				boolean syncMethodCalledOnThis = false;
				
				if (mCalled.isSynchronized() && s.getInvokeExpr() instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr instCall = (InstanceInvokeExpr)s.getInvokeExpr();
					Value callerObj = instCall.getBase();
					
					if (thisRef != null && Analysis.instance.mustAlias(ug, thisRef, firstStmt, callerObj, s)) {
						syncMethodCalledOnThis = true;
					}
				}
				
				for (Body bOther: Analysis.instance.findBodies(mCalled)) {
					if (!syncMethodCalledOnThis && bOther.getMethod().isSynchronized()) {
						isNested = true;
						break;
					}
					if (this.methodContainsLock(bOther, new HashSet<SootMethod>())) {
						isNested = true;
						break;
					}
				}				
				
				if (isNested)
					break;
			}			
		}
		
		
		if (isNested) {
			this.nestedSyncBlockPositions.add(positionFirst);
//			System.out.println(m+ " is nested");
			this.nSyncBlocksNested++;
		}
		else {
//			System.out.println(m+ " is not nested");
			this.nSyncBlocksUnnested++;
		}		
	}
	
	private boolean methodContainsLock(Body b, HashSet<SootMethod> explored) {
		SootMethod m = b.getMethod();
		
		if (methodsContainingLock.contains(m))
			return true;
		
		if (explored.contains(m))
			return false;
		explored.add(m);		

		if (Analysis.instance.containsLock(b)) {
			this.methodsContainingLock.add(m);
			return true;
		}
		
		for (Unit u: b.getUnits()) {
			Stmt s = (Stmt)u;			
			if (s.containsInvokeExpr()) {
				SootMethod mCalled = s.getInvokeExpr().getMethod();
				
				for (Body bOther: Analysis.instance.findBodies(mCalled)) {					
					if (bOther.getMethod().isSynchronized()) {
						this.methodsContainingLock.add(m);
						return true;
					}
					if (this.methodContainsLock(bOther, explored)) {
						this.methodsContainingLock.add(m);
						return true;
					}
				}								
			}			
		}
				
		return false;
	}	
	
	public void saveResults() {
//		System.out.println("saving positions of nested sync blocks/methods");

		try {
			PrintWriter pw = new PrintWriter("nested_locks");
			for (StackTraceElement p : this.nestedSyncBlockPositions) {
				pw.write(p.toString() + "\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}	
	
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("nested_locks"));
			String line;
			while ((line = br.readLine()) != null) {
				this.nestedSyncBlockPositions.add(Util.parsePosition(line));
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
