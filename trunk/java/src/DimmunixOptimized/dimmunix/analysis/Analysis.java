package dimmunix.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import dimmunix.Configuration;
import dimmunix.Pair;
import dimmunix.Vector;

public class Analysis {	
	public static final Analysis instance = new Analysis();
	
	public HashSet<String> loadedClasses;
	public HashSet<String> loadedClassesPreviousRun;		
	public HashSet<SootClass> loadedClassesSoot;	
	public boolean newLoadedClasses;	
	
	HashMap<SootClass, Vector<SootClass>> children;
	HashMap<SootMethod, Vector<Body>> bodiesCache;

	Analysis() {
		this.children = new HashMap<SootClass, Vector<SootClass>>();
		this.bodiesCache = new HashMap<SootMethod, Vector<Body>>();
		
		this.loadedClasses = new HashSet<String>();
		this.loadedClassesPreviousRun = new HashSet<String>();		
		this.loadedClassesSoot = new HashSet<SootClass>();				
	}

	public void init() {
		this.getLoadedClassesPreviousRun();		
	}
	
	public synchronized void checkIfThereAreNewLoadedClasses() {
		if (this.loadedClassesPreviousRun.containsAll(this.loadedClasses)) {
			this.newLoadedClasses = false;
		}
		else {
			this.loadedClasses.addAll(this.loadedClassesPreviousRun);
			this.newLoadedClasses = true;
		}
	}
	
	private void getLoadedClassesPreviousRun() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("loaded_classes"));
			String line;
			while ((line = br.readLine()) != null) {
				this.loadedClassesPreviousRun.add(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void saveLoadedClasses() {
//		System.out.println("saving loaded classes");
		try {
			PrintWriter pw = new PrintWriter("loaded_classes");
			for (String cl: this.loadedClasses)
				pw.write(cl + "\n");
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	synchronized void retreiveLoadedClasses() {
//		System.out.println("retrieving loaded classes");
		Vector<String> loadedClassesSnapshot = new Vector<String>(this.loadedClasses);
		for (String cl: loadedClassesSnapshot) {
			SootClass sootCl;
			synchronized (Scene.v()) {
				try {
					sootCl = Scene.v().loadClass(cl, SootClass.BODIES);
					if (!sootCl.isPhantomClass()) {
						this.loadedClassesSoot.add(sootCl);								
					}
				}
				catch (Throwable ex) {
//					System.out.println("could not retrieve soot class for "+ cl);
				}
			}
			
		}			
	}
	
	public boolean isWait(Stmt s) {
		return s.containsInvokeExpr() && s.getInvokeExpr().getMethodRef().name().equals("wait");
	}
	
	public String isClassInitStmt(Stmt s) {
		if (s.containsInvokeExpr()) {
			SootMethodRef mref = s.getInvokeExpr().getMethodRef();
			if (mref.name().contains("<init>")) {
//				System.out.println(mref.declaringClass().getName());
				return mref.declaringClass().getName();
			}			
		}
		if (s.containsFieldRef()) {
			SootFieldRef fref = s.getFieldRef().getFieldRef();
//			System.out.println(fref.declaringClass().getName());
			return fref.declaringClass().getName();
		}
		return null;
	}
	
	public boolean isWaitWithoutTimeout(Stmt s) {
		if (!s.containsInvokeExpr())
			return false;
		SootMethodRef mref = s.getInvokeExpr().getMethodRef();
		if (!mref.name().equals("wait"))
			return false;
		int numArgs = mref.parameterTypes().size();
		if (numArgs == 0) {
			System.out.println("wait without timeout");
		}
		else {
			System.out.println("wait with timeout");
		}
		return numArgs == 0;
	}	

	public boolean isNotify(Stmt s) {
		if (s.containsInvokeExpr()) {
			String metName = s.getInvokeExpr().getMethodRef().name();
			return metName.equals("notify") || metName.equals("notifyAll");
		}
		return false;
	}

	public boolean isLock(Stmt s) {
		return s instanceof EnterMonitorStmt;
	}

	public boolean isUnlock(Stmt s) {
		return s instanceof ExitMonitorStmt;
	}
	
	public boolean isExplicitLock(Stmt s) {
		if (s.containsInvokeExpr()) {
			String metName = s.getInvokeExpr().getMethodRef().name();
			String sig = s.getInvokeExpr().getMethodRef().getSignature();
			return metName.equals("lock") && sig.contains("java.util.concurrent.locks.");
		}		
		return false;
	}

	public boolean isExplicitUnlock(Stmt s) {
		if (s.containsInvokeExpr()) {
			String metName = s.getInvokeExpr().getMethod().getName();
			String className = s.getInvokeExpr().getMethod().getDeclaringClass().getName();
			return metName.equals("unlock") && className.startsWith("java.util.concurrent.locks.");
		}		
		return false;
	}

	public boolean containsLock(Body body) {
		for (Unit u : body.getUnits()) {
			Stmt s = (Stmt) u;
			if (this.isLock(s))
				return true;
		}
		return false;
	}
	
	public boolean containsNotify(Body body) {
		for (Unit u : body.getUnits()) {
			Stmt s = (Stmt) u;
			if (this.isNotify(s))
				return true;
		}
		return false;
	}
	
	public Stmt getStatement(StackTraceElement pos) {
		SootClass cl = null;
		for (SootClass c: this.loadedClassesSoot) {
			if (c.getName().equals(pos.getClassName())) {
				cl = c;
				break;
			}
		}
		if (cl == null) {
			System.out.println("could not get soot stmt for "+ pos);
			System.out.println("somehow class "+ pos.getClassName()+ " was not loaded");
			return null;
		}
		
		try {
			for (SootMethod m: cl.getMethods()) {
				if (m.getName().equals(pos.getMethodName())) {
					for (Unit u: m.retrieveActiveBody().getUnits()) {
						LineNumberTag lTag = (LineNumberTag)u.getTag("LineNumberTag");					
						if (lTag != null && lTag.getLineNumber() == pos.getLineNumber())
							return (Stmt)u;					
					}
				}
			}			
		}
		catch (Throwable ex) {
		}
		System.out.println("could not get soot stmt for "+ pos);
		return null;
	}
	
	public SootMethod getMethod(StackTraceElement pos) {
		SootClass cl = null;
		for (SootClass c: this.loadedClassesSoot) {
			if (c.getName().equals(pos.getClassName())) {
				cl = c;
				break;
			}
		}
		if (cl == null) {
			System.out.println("could not get soot stmt for "+ pos);
			System.out.println("somehow class "+ pos.getClassName()+ " was not loaded");
			return null;
		}
		
		try {
			for (SootMethod m: cl.getMethods()) {
				if (m.getName().equals(pos.getMethodName())) {
					for (Unit u: m.retrieveActiveBody().getUnits()) {
						LineNumberTag lTag = (LineNumberTag)u.getTag("LineNumberTag");					
						if (lTag != null && lTag.getLineNumber() == pos.getLineNumber())
							return m;					
					}
				}
			}			
		}
		catch (Throwable ex) {
		}
		System.out.println("could not get soot method for "+ pos);
		return null;
	}
	
	public ExceptionalUnitGraph getUnitGraph(StackTraceElement pos) {
		SootClass cl = null;
		for (SootClass c: this.loadedClassesSoot) {
			if (c.getName().equals(pos.getClassName())) {
				cl = c;
				break;
			}
		}
		if (cl == null) {
			System.out.println("could not get unit graph for "+ pos);
			System.out.println("somehow class "+ pos.getClassName()+ " was not loaded");
			return null;
		}
		
		try {
			for (SootMethod m: cl.getMethods()) {
				if (m.getName().equals(pos.getMethodName())) {
					for (Unit u: m.retrieveActiveBody().getUnits()) {
						LineNumberTag lTag = (LineNumberTag)u.getTag("LineNumberTag");					
						if (lTag != null && lTag.getLineNumber() == pos.getLineNumber())
							return new ExceptionalUnitGraph(m.getActiveBody());					
					}
				}
			}			
		}
		catch (Throwable ex) {
		}
		System.out.println("could not get unit graph for "+ pos);
		return null;
	}	
	
	public StackTraceElement getFirstStmtPosition(StackTraceElement frame, ExceptionalUnitGraph ug) {
		Unit s = ug.getHeads().get(0);
		int firstLine = 0;
		
		LineNumberTag lTag = (LineNumberTag)s.getTag("LineNumberTag");					
		if (lTag != null)
			firstLine = lTag.getLineNumber();					
		
		return new StackTraceElement(frame.getClassName(), frame.getMethodName(), frame.getFileName(), firstLine);
	}
	
	public Pair<ExceptionalUnitGraph, Unit> findFrame(String className, String methodName, int line) throws Throwable {
		SootClass cl = Scene.v().loadClass(className, SootClass.BODIES);
		if (cl.isPhantomClass())
			throw new Exception("could not find frame "+ className+ "."+ methodName+ ":"+ line);
		for (SootMethod m: cl.getMethods()) {			
			if (m.isConcrete() && m.getName().equals(methodName)) {
				Body body = m.retrieveActiveBody();
				for (Unit u: body.getUnits()) {
					LineNumberTag ltag = (LineNumberTag)u.getTag("LineNumberTag");
					if (ltag != null && ltag.getLineNumber() == line)
						return new Pair<ExceptionalUnitGraph, Unit>(new ExceptionalUnitGraph(body), u);
				}
			}
		}
		
		throw new Exception("could not find frame "+ className+ "."+ methodName+ ":"+ line);
	}
	
	Vector<SootClass> getAncestors(SootClass cl) {
		Vector<SootClass> ancestors = new Vector<SootClass>();
		
		if (cl.hasSuperclass()) {
			ancestors.add(cl.getSuperclass());
			ancestors.addAll(this.getAncestors(cl.getSuperclass()));
		}
		for (SootClass i: cl.getInterfaces()) {
			ancestors.add(i);
			ancestors.addAll(this.getAncestors(i));
		}
		return ancestors;
	}
	
	void findChildren() {
//		System.out.println("updating class hierarchy");
		
		for (SootClass cl: Analysis.instance.loadedClassesSoot) {
			Vector<SootClass> ancestors = this.getAncestors(cl); 
			for (SootClass a: ancestors) {
				Vector<SootClass> _children = this.children.get(a);
				if (_children == null) {
					_children = new Vector<SootClass>();
					this.children.put(a, _children);
				}
				if (!_children.contains(cl))
					_children.add(cl);
			}
		}
	}
	
	public Vector<Body> findBodies(SootMethod m) {
		Vector<Body> bodies = this.bodiesCache.get(m);
		if (bodies != null)
			return bodies;
			
		bodies = new Vector<Body>();
		this.bodiesCache.put(m, bodies);
		
		SootClass cl = m.getDeclaringClass();
		if (cl.isPhantomClass() || Configuration.instance.skip(cl.getName()))
			return bodies;
		
		if (m.isConcrete()) {
			try {
				bodies.add(m.retrieveActiveBody());
			}
			catch (Throwable ex) {
				System.out.println("failed to retrieve body of method "+ m);
			}
		}
		
		Vector<SootClass> _children = this.children.get(cl);
		if (_children != null) {
			for (SootClass child: _children) {
				if (child.isPhantomClass())
					continue;
				for (SootMethod mOther: child.getMethods()) {
					if (mOther.isConcrete() && mOther.getSubSignature().equals(m.getSubSignature())) {
						try {
							bodies.add(mOther.retrieveActiveBody());
						}
						catch (Throwable ex) {
							System.out.println("failed to retrieve body of method "+ mOther);
						}
					}
				}				
			}
		}
		
		return bodies;
	}
	
	public void run() {
		this.retreiveLoadedClasses();
		this.findChildren();
	}
	
	public boolean isInCycle(ExceptionalUnitGraph ug, Unit s) {
		return this.isInCycle(ug, s, s, new Vector<Unit>(100));
	}
	
	private boolean isMethodCallInCycle(SootMethod m0, Body body, HashSet<SootMethod> exploring) {
		if (exploring.contains(body.getMethod()) || m0 == body.getMethod())
			return true;
		exploring.add(body.getMethod());
		
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
					for (Body b: this.findBodies(m)) {
						if (this.isMethodCallInCycle(m0, b, exploring)) {
							return true;
						}
					}
				}				
			}
		}
		
		return false;
	}
	
	private boolean isInCycle(ExceptionalUnitGraph ug, Unit s0, Unit curStm, Vector<Unit> traversed) {
		if (traversed.contains(curStm))
			return false;
		traversed.add(curStm);
		
		Stmt sCur = (Stmt)curStm;
		if (sCur.containsInvokeExpr()) {
			SootMethod m = null;
			try {
				m = sCur.getInvokeExpr().getMethod();
			}
			catch (Throwable ex) {				
			}
			if (m != null) {
				for (Body b: this.findBodies(m)) {
					if (this.isMethodCallInCycle(ug.getBody().getMethod(), b, new HashSet<SootMethod>())) {
						return true;
					}
				}
			}
		}
		
		for (Unit snext : ug.getSuccsOf(curStm)) {
			if (snext == s0)
				return true;
			if (isInCycle(ug, s0, snext, traversed))
				return true;
		}
		return false;
	}
	
	public boolean mustAliasLockCall(ExceptionalUnitGraph ug, EnterMonitorStmt sLock, Stmt sCall) {		
		return this.mustAlias(ug, sLock.getOp(), sLock, ((InstanceInvokeExpr) sCall.getInvokeExpr()).getBase(), sCall);
	}

	public boolean mustAliasLockUnlock(ExceptionalUnitGraph ug, EnterMonitorStmt sLock, Stmt sUnlock) {
		return this.mustAlias(ug, (Local) sLock.getOp(), sLock, (Local) ((ExitMonitorStmt) sUnlock).getOp(), sUnlock);
	}
	
	public boolean mustAlias(ExceptionalUnitGraph ug, Value x1, Stmt s1, Value x2, Stmt s2) {
		if (x1 == x2)
			return true;
		
		Vector<Assignment> assignments = new Vector<Assignment>();
		HashSet<Stmt> explored = new HashSet<Stmt>();

		Unit firstStm = ug.getHeads().get(0);
		return this.myMustAliasRec(ug, (Stmt)firstStm, x1, s1, x2, s2, assignments, explored, false);
	}

	private boolean myMustAliasRec(ExceptionalUnitGraph ug, Stmt s, Value x1, Stmt s1, Value x2, Stmt s2, Vector<Assignment> assignments, HashSet<Stmt> explored, boolean s1Reached) {
		if (explored.contains(s))
			return true;
		explored.add(s);

		s1Reached = s1Reached || s == s1;

		if (s1Reached && s == s2) {
			HashMap<Value, Value> asgnMap = this.processAssignments(assignments);
			return getRootValue(x1, asgnMap).toString().equals(getRootValue(x2, asgnMap).toString());
		}

		if (s instanceof AssignStmt) {
			AssignStmt asgn = (AssignStmt)s;
			assignments.add(new Assignment(asgn.getLeftOp(), asgn.getRightOp()));
		}

		try {
			List<Unit> succs = ug.getUnexceptionalSuccsOf(s);	
			for (Unit snext: succs) {
				if (!myMustAliasRec(ug, (Stmt) snext, x1, s1, x2, s2, assignments, explored, s1Reached))
					return false;
			}
			return true;
		}
		finally {
			//pop assignment for backtracking
			if (s instanceof AssignStmt) {
				assignments.remove(assignments.size()- 1);
			}			
		}
	}
	
	private HashMap<Value, Value> processAssignments(Vector<Assignment> assignments) {
		HashMap<Value, Value> asgnMap = new HashMap<Value, Value>();

		for (Assignment a: assignments) {
			asgnMap.put(a.leftValue, a.rightValue);
		}

		return asgnMap;
	}

	private Value getRootValue(Value x, HashMap<Value, Value> assignments) {
		Value v = assignments.get(x);

		if (v == null)
			return x;
		else
			return this.getRootValue(v, assignments);
	}
	
	public StackTraceElement getPosition(SootMethod m, Stmt s) {
		LineNumberTag line = (LineNumberTag) s.getTag("LineNumberTag");
		SourceFileTag srcFile = (SourceFileTag) m.getDeclaringClass().getTag("SourceFileTag");

		if (line != null && srcFile != null) {
			return new StackTraceElement(m.getDeclaringClass().getName(), m.getName(), srcFile.getSourceFile(), line.getLineNumber());
		}

		return null;
	}
}
