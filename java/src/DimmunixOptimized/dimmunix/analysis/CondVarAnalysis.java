package dimmunix.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import org.aspectj.weaver.loadtime.ExecutionPositions;
import org.aspectj.weaver.loadtime.SyncPositions;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.Util;

public class CondVarAnalysis {
	public static final CondVarAnalysis instance = new CondVarAnalysis();	
	
	private final HashSet<StackTraceElement> allLockBeforeNotifyPositions = new HashSet<StackTraceElement>();	
	
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("all_lock_before_notify_positions"));
			String line;
			
			while ((line = br.readLine()) != null) {
				this.allLockBeforeNotifyPositions.add(Util.parsePosition(line));
			}
			
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	public void saveResults() {
		try {
			PrintWriter pw = new PrintWriter("all_lock_before_notify_positions");
			
			for (StackTraceElement p: this.allLockBeforeNotifyPositions) {
				pw.println(p);
			}
			
			pw.close();
		} catch (IOException e) {
		}
	}
	
	public boolean isLockBeforeNotifyPosition(StackTraceElement p) {
		return this.allLockBeforeNotifyPositions.contains(p);
	}
	
	public void run() {
		for (SootClass cl: Analysis.instance.loadedClassesSoot) {
			this.run(cl);
		}		
	}
	
	private void run(SootClass cl) {
		for (SootMethod m : cl.getMethods()) {
			if (m.isConcrete()) {										
				Body body;
				try {
					body = m.retrieveActiveBody();
				}
				catch (Throwable ex) {
					System.out.println("could not retrieve body of method "+ m);
					continue;
				}
				ExceptionalUnitGraph ug;
				try {
					ug = new ExceptionalUnitGraph(body);
				}
				catch (Throwable ex) {
					System.out.println("could not retrieve unit graph of method "+ m);
					continue;
				}
				
				this.findWaitPositions(m, ug);
				this.findLockBeforeNotifyPositions(m, ug);
			}
		}
	}
	
	private void findWaitPositions(SootMethod m, ExceptionalUnitGraph ug) {
		for (Unit u: ug.getBody().getUnits()) {
			Stmt s = (Stmt) u;
			if (Analysis.instance.isWait(s)) {
				StackTraceElement p = Analysis.instance.getPosition(m, s);
				if (p != null) {
					ExecutionPositions.instance.addPosition(p);					
				}
			}
		}
	}
	
	private void findLockBeforeNotifyPositions(SootMethod m, ExceptionalUnitGraph ug) {
		Body body = ug.getBody();
		if (!Analysis.instance.containsLock(body) || !Analysis.instance.containsNotify(body))
			return;

		for (Unit u : body.getUnits()) {
			Stmt s = (Stmt) u;
			if (Analysis.instance.isLock(s)) {
				EnterMonitorStmt lockStm = (EnterMonitorStmt) s;
				if (this.notifyReachableWithin(ug, lockStm)) {
					StackTraceElement p = Analysis.instance.getPosition(m, lockStm);
					if (p != null) {
						ExecutionPositions.instance.addPosition(p);
						SyncPositions.add(p);
						this.allLockBeforeNotifyPositions.add(p);
					}
				}
			}
		}
		
	}
	
	private boolean notifyReachableWithin(ExceptionalUnitGraph ug, EnterMonitorStmt s) {
		HashSet<Stmt> explored = new HashSet<Stmt>();
		return this.notifyReachableWithinRec(ug, s, s, explored);
	}

	private boolean notifyReachableWithinRec(ExceptionalUnitGraph ug, EnterMonitorStmt start, Stmt s, HashSet<Stmt> explored) {
		if (explored.contains(s))
			return true;
		explored.add(s);

		if (Analysis.instance.isUnlock(s)) {
			return false;
		}
		if (Analysis.instance.isNotify(s) && Analysis.instance.mustAliasLockCall(ug, start, s)) {
			return true;
		}

		List<Unit> succs = ug.getUnexceptionalSuccsOf(s);
		for (Unit snext : succs) {
			if (this.notifyReachableWithinRec(ug, start, (Stmt) snext, explored))
				return true;
		}
		return false;
	}
}
