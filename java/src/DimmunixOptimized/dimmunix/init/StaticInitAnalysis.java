package dimmunix.init;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.StringTokenizer;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.Pair;
import dimmunix.Util;
import dimmunix.analysis.Analysis;

public class StaticInitAnalysis {
	
	public static final StaticInitAnalysis instance = new StaticInitAnalysis();
	
	private final HashSet<Pair<StackTraceElement, String>> initPositions = new HashSet<Pair<StackTraceElement,String>>();
	
	private StaticInitAnalysis() {		
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
				
				this.findInitStmts(m, ug);
			}
		}
	}

	private void findInitStmts(SootMethod m, ExceptionalUnitGraph ug) {
		for (Unit u: ug.getBody().getUnits()) {
			Stmt s = (Stmt) u;
			String cls;
			if ((cls = Analysis.instance.isClassInitStmt(s)) != null) {
				StackTraceElement p = Analysis.instance.getPosition(m, s);
				if (p != null) {
					this.initPositions.add(new Pair<StackTraceElement, String>(p, cls));
				}
			}
		}
	}

	public void saveResults() {		
		try {
			PrintWriter pw = new PrintWriter("init_positions");
			
			for (Pair<StackTraceElement, String> p: this.initPositions) {
				pw.println(p);
			}
			
			pw.close();
		} catch (FileNotFoundException e) {
		}
	}
	
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("init_positions"));

			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer stok = new StringTokenizer(line, ",");
				StackTraceElement pos = Util.parsePosition(stok.nextToken()); 
				String cls = stok.nextToken();
				this.initPositions.add(new Pair<StackTraceElement, String>(pos, cls));
			}
			
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	public String isInitPosition(StackTraceElement pos) {
		for (Pair<StackTraceElement, String> p: this.initPositions) {
			if (p.v1.equals(pos)) {
				return p.v2;
			}
		}
		
		return null;
	}
}
