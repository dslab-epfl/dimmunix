/*
     Created by Saman A. Zonouz, Horatiu Jula, Pinar Tozun, Cristina Basescu, George Candea
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix Vaccination Framework.

     Dimmunix Vaccination Framework is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix Vaccination Framework is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix Vaccination Framework. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

package evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Unit;
import soot.Value;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;
import dIV.core.Validator;
import dIV.core.staticAnalysis.CallFrame;
import dIV.core.staticAnalysis.LockEvent;
import dIV.core.staticAnalysis.Signature;
import dIV.core.staticAnalysis.StaticAnalysis;
import dIV.interf.IStaticAnalyzer;
import dIV.util.StaticAnswer;

/**
 * Evaluation for the signatures that can pass the call graph filter
 * 
 */
public class Evaluation {

	/** Call graph of the evaluated program */
	private CallGraph callGraph;

	/** Classes for the evaluated program */
	private Vector<SootClass> classes = new Vector<SootClass>();

	/** Enter monitor frames for the evaluated program */
	private Vector<CallFrame> enterMonitorFrames = new Vector<CallFrame>();

	/** Lock events corresponding to the enter monitor frames */
	private HashMap<CallFrame, LockEvent> enterMonitorLockEvents = new HashMap<CallFrame, LockEvent>();

	/** Cache for isMethodReachable */
	HashMap<SootMethod, HashMap<SootMethod, Boolean>> methodReachabilityMap = new HashMap<SootMethod, HashMap<SootMethod, Boolean>>();

	/** Cache for isFrameReachable */
	HashMap<CallFrame, HashSet<CallFrame>> frameReachabilityMap = new HashMap<CallFrame, HashSet<CallFrame>>();

	/** Keeps the already created StrongLocalMustAliasAnalysis objects for a method */
	private HashMap<SootMethod, StrongLocalMustAliasAnalysis> methodAliasAnalyses = new HashMap<SootMethod, StrongLocalMustAliasAnalysis>();

	/** MayAlias cache */
	private HashMap<Value, HashMap<Value, Boolean>> mayAliasMap = new HashMap<Value, HashMap<Value, Boolean>>();

	/** Class-SubClasses */
	private HashMap<SootClass, Vector<SootClass>> subClasses = new HashMap<SootClass, Vector<SootClass>>();

	/** Class-SuperClasses */
	private HashMap<SootClass, Vector<SootClass>> superClasses = new HashMap<SootClass, Vector<SootClass>>();

	/** Random number generator */
	private Random r = new Random(System.currentTimeMillis());

	public Evaluation() {
		this.setupSoot();
	}

	/**
	 * Prepares interaction with soot
	 */
	private void setupSoot() {
		Options.v().set_whole_program(false);
		Options.v().set_via_shimple(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(true);
		Options.v().set_src_prec(Options.src_prec_only_class);
		Options.v().set_app(false);
		// Options.v().set_debug_resolver(true);
		// Options.v().set_allow_phantom_refs(true);
	}

	/**
	 * Initializes control flow graph
	 */
	private void buildCallGraph() {
		CallGraphBuilder cgb = new CallGraphBuilder();
		cgb.build();
		this.callGraph = cgb.getCallGraph();
		//System.out.println("call graph built");
	}

	/**
	 * Gets classes under the given path
	 * 
	 * @param paths
	 */
	private void getClasses(String[] paths) {
		Vector<String> v_cls = new Vector<String>();
		for (String p : paths) {
			//System.out.println(p);
			v_cls.addAll(SourceLocator.v().getClassesUnder(p));
		}

		SootClass cl;
		for (String c : v_cls) {
			//System.out.println(c);
			cl = Scene.v().loadClass(c, SootClass.BODIES);
			cl.setApplicationClass();
			classes.add(cl);
		}

		Scene.v().loadDynamicClasses();

		this.buildCallGraph();
	}

	/**
	 * Load classes in the dirs given in the file
	 * 
	 * @param file
	 */
	private void getClasses(String file, String prePath) {
		try {
			BufferedReader pathsFile = new BufferedReader(new FileReader(file));
			String classpath = pathsFile.readLine();
			String[] paths = classpath.split(":");
			for (int i = 0; i < paths.length; i++)
				paths[i] = new String(prePath + paths[i]);
			this.getClasses(paths);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Counts the number of methods in the classes
	 * 
	 * @return method count
	 */
	private int getMethodCount() {
		int mcount = 0;
		for (SootClass cl : classes) {
			if (!cl.getPackageName().contains("testsuite"))
				mcount += cl.getMethodCount();
		}
		return mcount;
	}

	/**
	 * Gets enter monitor frames from the evaluated program and stores them in enterMonitorFrames. Enter monitor frames are either the
	 * starting point of a synchronized block or a place that a synchronized method is called.
	 */
	private void getEnterMonitorFrames() {
		for (SootClass c : this.classes) {
			if (!c.getPackageName().contains("testsuite")) {
				for (SootMethod m : c.getMethods()) {
					if (m.isConcrete()) {
						if (m.isSynchronized()) {
							for (Iterator<Edge> edge = this.callGraph.edgesInto(m); edge.hasNext();) {
								SootMethod crtMethod = edge.next().src();
								if (!crtMethod.getDeclaringClass().getPackageName().contains("testsuite")) {
									for (Unit u : crtMethod.retrieveActiveBody().getUnits()) {
										Stmt s = (Stmt) u;
										if (s.containsInvokeExpr()) {
											if (s.getInvokeExpr().getMethod().getSubSignature().equals(m.getSubSignature()))
												this.enterMonitorFrames.add(new CallFrame(crtMethod, s));
										}
									}
								}
							}
						} else {
							for (Unit u : m.retrieveActiveBody().getUnits()) {
								Stmt crtStmt = (Stmt) u;
								if (crtStmt instanceof EnterMonitorStmt)
									this.enterMonitorFrames.add(new CallFrame(m, crtStmt));
							}
						}
					}
				}
			}
		}
		System.out.println("# entermonitor frames = " + this.enterMonitorFrames.size());
	}

	/**
	 * Creates the lock events corresponding to all enterMonitorFrames
	 */
	private void createLockEvents() {
		for (CallFrame f : this.enterMonitorFrames)
			this.enterMonitorLockEvents.put(f, this.createLockEvent(f));
	}

	/**
	 * Creates the lock event for the given frame f
	 * 
	 * @param f
	 */
	private LockEvent createLockEvent(CallFrame f) {
		if (f.stmt.containsInvokeExpr()) {
			SootMethod m = f.stmt.getInvokeExpr().getMethod();
			int lineNumber = f.lineNumber;
			Value v;
			try {
				v = m.retrieveActiveBody().getThisLocal();
			} catch (RuntimeException e) {
				return null;
			}
			Stmt s = new JEnterMonitorStmt(v);
			s.addTag(new LineNumberTag(lineNumber));
			return new LockEvent(m, s, true);
		} else
			return new LockEvent(f.method, f.stmt, false);
	}

	/**
	 * Adds the given class c to the classes
	 * 
	 * @param c
	 */
	private void addClass(String c) {
		SootClass cl = Scene.v().loadClass(c, SootClass.BODIES);
		cl.setApplicationClass();
		this.classes.add(cl);
		this.buildCallGraph();
	}

	public void getRandomCallStacks(Vector<CallFrame> curStack, int size, Vector<Vector<CallFrame>> stacks) {
		// System.out.println("getCallStacks");
		for (int i = 0; i < size; i++)
			curStack.add(this.randFrame());
		stacks.add(new Vector<CallFrame>(curStack));
	}

	public Vector<String> formRandomSignatures(Vector<Vector<CallFrame>> stack) {
		Vector<String> sigs = new Vector<String>();
		Collections.shuffle(stack);
		for (Vector<CallFrame> f1 : stack) {
			Collections.shuffle(stack);
			for (Vector<CallFrame> f2 : stack) {
				// deadlock_template=depth1#call_stack_outer1;depth2#call_stack_outer2
				String call_stack_outer1 = "";
				for (CallFrame f : f1)
					call_stack_outer1 = call_stack_outer1 + f.toString() + ",";
				call_stack_outer1 = call_stack_outer1.substring(0, call_stack_outer1.length() - 1);
				// System.out.println("");
				String call_stack_outer2 = "";
				for (CallFrame f : f2)
					call_stack_outer2 = call_stack_outer2 + f.toString() + ",";
				call_stack_outer2 = call_stack_outer2.substring(0, call_stack_outer2.length() - 1);
				String s = "deadlock_template=1#" + call_stack_outer1 + ";1#" + call_stack_outer2;
				sigs.add(s);
				// System.out.println(s);
				break;
			}
			if (sigs.size() > 1000)
				break;
			Collections.shuffle(stack);
		}
		return sigs;
	}

	/**
	 * @return a random chosen class from this.classes
	 */
	private SootClass randClass() {
		int noClasses = classes.size();
		int index = this.r.nextInt(noClasses);
		return classes.get(index);
	}

	/**
	 * @return a random generated CallFrame
	 */
	public CallFrame randFrame() {

		while (true) {
			// choose a random class
			SootClass c = this.randClass();
			// choose a random method from class c
			int noMeth = c.getMethodCount();
			if (noMeth == 0)
				continue;
			int index = this.r.nextInt(noMeth);

			// loop all methods from c
			int count = 0;
			for (SootMethod m : c.getMethods()) {
				// if m is the randomly selected method
				if (count >= index) {

					// if it is not concrete, randomly generate a line number
					if (!m.isConcrete() || m.isNative()) {
						continue;
					}

					// else count the statements
					int noStmts = 0;
					for (Unit u : m.retrieveActiveBody().getUnits()) {
						noStmts++;
					}
					/*
					 * randomly select a statement, but also allow to generate a line number that doesn't exist in m, with a probability of
					 * 10%
					 */
					int stmtIndex = this.r.nextInt(noStmts + noStmts / 10);
					int count2 = 0;
					for (Unit u : m.retrieveActiveBody().getUnits()) {
						Stmt crtStmt = (Stmt) u;
						if (count2 == stmtIndex)
							return new CallFrame(m, crtStmt);
						count2++;
					}
					return new CallFrame(m, stmtIndex);

				}
				count++;
			}

			// shouldn't get here
			// throw new
			// RuntimeException("failed to generate random CallFrame");
		}
	}

	/**
	 * Creates feasible call stacks taking curFrame as the top of the stack and with the given depth till nStacks call stacks are created
	 * 
	 * @param curFrame
	 * @param curStack
	 * @param depth
	 * @param size
	 * @param stacks
	 */
	private void getCallStacks(CallFrame curFrame, Vector<CallFrame> curStack, int depth, int size, Vector<Vector<CallFrame>> stacks,
			int nStacks) {
		// System.out.println("getCallStacks");
		if (stacks.size() == nStacks)
			return;
		if (depth == size) {
			stacks.add(new Vector<CallFrame>(curStack));
			return;
		}
		for (Iterator<Edge> edge = this.callGraph.edgesInto(curFrame.method); edge.hasNext();) {
			SootMethod m = edge.next().src();
			for (Unit u : m.retrieveActiveBody().getUnits()) {
				Stmt s = (Stmt) u;
				if (s.containsInvokeExpr()) {
					SootMethod mNext = s.getInvokeExpr().getMethod();
					if (mNext.getSubSignature().equals(curFrame.method.getSubSignature())) {
						CallFrame nextFrame = new CallFrame(m, s);
						curStack.add(nextFrame);
						getCallStacks(nextFrame, curStack, depth + 1, size, stacks, nStacks);
						curStack.remove(curStack.size() - 1);
					}
				}
			}
		}
	}

	/**
	 * Counts all feasible call stacks taking method as the top of the stack and with the given depth
	 * 
	 * @param maps_nstacks
	 * @param method
	 * @param depth
	 * @param size
	 */
	private int countCallStacks(Vector<HashMap<SootMethod, Integer>> maps_nstacks, SootMethod method, int depth, int size) {
		if (depth == size)
			return 1;
		Integer nstacks = maps_nstacks.get(depth - 1).get(method);
		if (nstacks != null)
			return nstacks;
		int n = 0;
		for (Iterator<Edge> edge = this.callGraph.edgesInto(method); edge.hasNext();) {
			SootMethod m = edge.next().src();
			for (Unit u : m.retrieveActiveBody().getUnits()) {
				Stmt s = (Stmt) u;
				if (s.containsInvokeExpr()) {
					SootMethod mNext = s.getInvokeExpr().getMethod();
					if (mNext.getSubSignature().equals(method.getSubSignature()))
						n += countCallStacks(maps_nstacks, m, depth + 1, size);
				}
			}
		}
		maps_nstacks.get(depth - 1).put(method, n);
		return n;
	}

	/**
	 * Checks whether the inner frame is reachable from the outer frame
	 * 
	 * @param outer
	 * @param inner
	 */
	private boolean isFrameReachable(CallFrame outer, CallFrame inner) {
		if (this.frameReachabilityMap.containsKey(outer) && this.frameReachabilityMap.get(outer).contains(inner))
			return true;
		boolean result = false;
		SootMethod mInner = inner.method;
		if (outer.stmt.containsInvokeExpr()) {
			// SootMethod mOuter = outer.stmt.getInvokeExpr().getMethod();
			// result = mOuter == mInner || this.isMethodReachable(mOuter, mInner, 10);
		} else {
			SootMethod mOuter = outer.method;
			Stmt sOuter = outer.stmt;
			LockEvent outerLockEvent = new LockEvent(mOuter, sOuter, false);
			Chain<Unit> stmts = mOuter.retrieveActiveBody().getUnits();
			Iterator<Unit> stmtsIterator = stmts.iterator();
			while (((Stmt) stmtsIterator.next()) != sOuter)
				System.out.println(sOuter);
			// HashSet<SootMethod> methodsCalled = new HashSet<SootMethod>();
			System.out.println("--------------------------------CheckingInner------------------------");
			Stmt crtStmt;
			try {
				while (!result && (crtStmt = (Stmt) stmtsIterator.next()) != null
						&& !(crtStmt instanceof ExitMonitorStmt && this.mustAlias(new LockEvent(mOuter, crtStmt, false), outerLockEvent))) {
					System.out.println(crtStmt);
					if (crtStmt == inner.stmt)
						result = true;
					// else if (crtStmt.containsInvokeExpr())
					// methodsCalled.add(crtStmt.getInvokeExpr().getMethod());
				} /*
				 * if (!result) { for (SootMethod m : methodsCalled) { if (m == mInner || this.isMethodReachable(m, mInner, 10)) { result =
				 * true; break; } } }
				 */
			} catch (Exception e) {
				System.out.println("exception thrown");
			}
		}
		if (result) {
			HashSet<CallFrame> map = this.frameReachabilityMap.get(outer);
			if (map == null) {
				map = new HashSet<CallFrame>();
				this.frameReachabilityMap.put(outer, map);
			}
			map.add(inner);
		}
		return result;
	}

	/**
	 * Checks whether the inner frame is reachable from the outer frame
	 * 
	 * @param outer
	 * @param inner
	 */
	private int isFrameReachableEfficient(CallFrame outer) {
		// if (this.frameReachabilityMap.containsKey(outer) && this.frameReachabilityMap.get(outer).contains(inner))
		// return true;
		// boolean result = false;
		int count = 0;
		// SootMethod mInner = inner.method;
		if (outer.stmt.containsInvokeExpr()) {
			// SootMethod mOuter = outer.stmt.getInvokeExpr().getMethod();
			// result = mOuter == mInner || this.isMethodReachable(mOuter, mInner, 10);
		} else {
			SootMethod mOuter = outer.method;
			Stmt sOuter = outer.stmt;
			LockEvent outerLockEvent = new LockEvent(mOuter, sOuter, false);
			Chain<Unit> stmts = mOuter.retrieveActiveBody().getUnits();
			Iterator<Unit> stmtsIterator = stmts.iterator();
			while (((Stmt) stmtsIterator.next()) != sOuter)
				System.out.println(sOuter);
			// HashSet<SootMethod> methodsCalled = new HashSet<SootMethod>();
			System.out.println("--------------------------------CheckingInner------------------------");
			Stmt crtStmt;
			HashSet<Stmt> synchBlockStmts = new HashSet<Stmt>();
			try {
				while (/* !result && */(crtStmt = (Stmt) stmtsIterator.next()) != null
						&& !(crtStmt instanceof ExitMonitorStmt && this.mustAlias(new LockEvent(mOuter, crtStmt, false), outerLockEvent))) {
					synchBlockStmts.add(crtStmt);
					System.out.println(crtStmt);
					// if (crtStmt == inner.stmt)
					// result = true;
					// else if (crtStmt.containsInvokeExpr())
					// methodsCalled.add(crtStmt.getInvokeExpr().getMethod());
				} /*
				 * if (!result) { for (SootMethod m : methodsCalled) { if (m == mInner || this.isMethodReachable(m, mInner, 10)) { result =
				 * true; break; } } }
				 */
			} catch (Exception e) {
				System.out.println("exception thrown");
			}
			for (CallFrame inner : this.enterMonitorFrames) {
				if (synchBlockStmts.contains(inner.stmt)) {
					HashSet<CallFrame> map = this.frameReachabilityMap.get(outer);
					if (map == null) {
						map = new HashSet<CallFrame>();
						this.frameReachabilityMap.put(outer, map);
					}
					map.add(inner);
					count++;
				}
			}
		} /*
		 * if (result) { HashSet<CallFrame> map = this.frameReachabilityMap.get(outer); if (map == null) { map = new HashSet<CallFrame>();
		 * this.frameReachabilityMap.put(outer, map); } map.add(inner); }
		 */
		// return result;
		return count;
	}

	// TODO: eliminate duplicates with StaticAnalysis class because there are many of them now

	/**
	 * 
	 * @param m
	 * @return the StrongLocalMustAliasAnalysis object for the given method m from methodAliasAnalyses
	 */
	private StrongLocalMustAliasAnalysis getAliasAnalysis(SootMethod m) {
		StrongLocalMustAliasAnalysis a = this.methodAliasAnalyses.get(m);
		if (a == null) {
			a = new StrongLocalMustAliasAnalysis(new BriefUnitGraph(m.getActiveBody()));
			this.methodAliasAnalyses.put(m, a);
		}
		return a;
	}

	/**
	 * Checks whether the synchronized objects from l1 and l2 must alias
	 * 
	 * @param l1
	 * @param l2
	 * @return true, if l1 and l2 must alias; false, otherwise
	 */
	private boolean mustAlias(LockEvent l1, LockEvent l2) {
		StrongLocalMustAliasAnalysis mustAliasAnalysis = this.getAliasAnalysis(l1.method);
		return mustAliasAnalysis.mustAlias((Local) ((JExitMonitorStmt) l1.stmt).getOp(), l1.stmt, (Local) ((JEnterMonitorStmt) l2.stmt)
				.getOp(), l2.stmt);
	}

	private Vector<SootClass> getSuperClasses(SootClass cls) {
		if (this.superClasses.containsKey(cls))
			return this.superClasses.get(cls);
		else {
			Vector<SootClass> superClasses = new Vector<SootClass>();

			superClasses.addAll(cls.getInterfaces());

			while (cls.hasSuperclass()) {
				superClasses.add(cls.getSuperclass());
				cls = cls.getSuperclass();
				superClasses.addAll(cls.getInterfaces());
			}
			this.superClasses.put(cls, superClasses);
			return superClasses;
		}
	}

	private Vector<SootClass> getSubClasses(SootClass cls) {
		if (this.subClasses.containsKey(cls))
			return this.subClasses.get(cls);
		else {
			Vector<SootClass> subClasses = new Vector<SootClass>();
			Stack<SootClass> classesToBeProcessed = new Stack<SootClass>();

			SootClass currentClass;
			classesToBeProcessed.add(cls);

			while (!classesToBeProcessed.empty()) {
				currentClass = classesToBeProcessed.pop();
				for (SootClass cl : this.classes) {
					Vector<SootClass> superClasses = this.getSuperClasses(cl);
					if (superClasses.contains(currentClass)) {
						subClasses.add(cl);
						classesToBeProcessed.add(cl);
					}
				}
			}
			this.subClasses.put(cls, subClasses);
			return subClasses;
		}
	}

	private boolean mayAlias(LockEvent l1, LockEvent l2) {
		if (l1 == null || l2 == null)
			return false;
		// May Alias Cache Optimization
		Value v1 = ((JEnterMonitorStmt) l1.stmt).getOp();
		Value v2 = ((JEnterMonitorStmt) l2.stmt).getOp();
		if (this.mayAliasMap.containsKey(v1) && this.mayAliasMap.get(v1).containsKey(v2))
			return this.mayAliasMap.get(v1).get(v2);
		else if (this.mayAliasMap.containsKey(v2) && this.mayAliasMap.get(v2).containsKey(v1))
			return this.mayAliasMap.get(v2).get(v1);
		else {
			boolean result = true;

			// Type Check Optimization
			String type1 = v1.getType().toString();
			// System.out.println(type1);
			String type2 = v2.getType().toString();
			// System.out.println(type2);
			try {
				SootClass class1 = Scene.v().getSootClass(type1);
				SootClass class2 = Scene.v().getSootClass(type2);
				if (class1 != class2 && (!this.getSubClasses(class1).contains(class2) || !this.getSubClasses(class2).contains(class1)))
					result = false;
			} catch (RuntimeException e) {
				if (!type1.equals(type2))
					result = false;
			}
			/*
			 * try { result = this.mayAliasAnalysis.mayAlias(l1.method, v1, l2.method, v2); } catch (RuntimeException e) { result = false; }
			 */
			// MayAlias Cache
			if (this.mayAliasMap.get(v1) == null)
				this.mayAliasMap.put(v1, new HashMap<Value, Boolean>());
			this.mayAliasMap.get(v1).put(v2, result);
			if (this.mayAliasMap.get(v2) == null)
				this.mayAliasMap.put(v2, new HashMap<Value, Boolean>());
			this.mayAliasMap.get(v2).put(v1, result);
			//
			return result;
		}
	}

	/**
	 * Checks whether the inner method is reachable from the outer method
	 * 
	 * @param outer
	 * @param inner
	 */
	private boolean isMethodReachable(SootMethod outer, SootMethod inner, int i) {
		if (i > 0) {
			if (this.methodReachabilityMap.containsKey(outer) && this.methodReachabilityMap.get(outer).containsKey(inner))
				return this.methodReachabilityMap.get(outer).get(inner);
			else {
				boolean isReachable = false;
				HashMap<SootMethod, Boolean> outerReachabilityMap = this.methodReachabilityMap.get(outer);
				if (outerReachabilityMap == null) {
					outerReachabilityMap = new HashMap<SootMethod, Boolean>();
					this.methodReachabilityMap.put(outer, outerReachabilityMap);
				}
				SootMethod m;
				for (Iterator<Edge> edge = this.callGraph.edgesOutOf(outer); !isReachable && edge.hasNext();) {
					m = edge.next().tgt();
					if (!outerReachabilityMap.containsKey(m)) {
						outerReachabilityMap.put(m, true);
						if (m == inner)
							return true;
						else
							isReachable = this.isMethodReachable(m, inner, i--);
					}
				}
				outerReachabilityMap.put(inner, isReachable);
				return isReachable;
			}
		}
		return false;
	}

	/**
	 * Forms all the possible signatures for the given feasible call stacks
	 * 
	 * @param stack
	 * @return formed signatures list
	 */
	private Vector<String> formSignatures(Vector<Vector<CallFrame>> stack) {
		Vector<String> sigs = new Vector<String>();
		for (Vector<CallFrame> f1 : stack) {
			for (Vector<CallFrame> f2 : stack) {
				// deadlock_template=depth1#call_stack_outer1;depth2#call_stack_outer2
				String call_stack_outer1 = "";
				for (CallFrame f : f1)
					call_stack_outer1 = call_stack_outer1 + f.toString() + ",";
				call_stack_outer1 = call_stack_outer1.substring(0, call_stack_outer1.length() - 1);
				// System.out.println("");
				String call_stack_outer2 = "";
				for (CallFrame f : f2)
					call_stack_outer2 = call_stack_outer2 + f.toString() + ",";
				call_stack_outer2 = call_stack_outer2.substring(0, call_stack_outer2.length() - 1);
				sigs.add("deadlock_template=1#" + call_stack_outer1 + ";1#" + call_stack_outer2);
			}
		}
		return sigs;
	}

	// inner
	private int countPassedSigsCFGF() {
		int count = 0;
		for (CallFrame fOuter1 : this.frameReachabilityMap.keySet()) {
			for (CallFrame fOuter2 : this.frameReachabilityMap.keySet()) {
				for (CallFrame fInner1 : this.frameReachabilityMap.get(fOuter1)) {
					for (CallFrame fInner2 : this.frameReachabilityMap.get(fOuter2)) {
						if (this.mayAlias(this.enterMonitorLockEvents.get(fOuter1), this.enterMonitorLockEvents.get(fInner2))
								&& this.mayAlias(this.enterMonitorLockEvents.get(fOuter2), this.enterMonitorLockEvents.get(fInner1)))
							count++;
						System.out.println(count);
					}
				}
			}
		}
		return count;
	}

	// inner
	private int countPassedSigsCGF() {
		int sigs = 0;
		for (CallFrame fOuter : this.enterMonitorFrames) {
			for (CallFrame fInner : this.enterMonitorFrames) {
				if (fOuter != fInner && this.isFrameReachable(fOuter, fInner))
					sigs++;
				System.out.println(sigs);
			}
		}
		return sigs;
	}

	// inner
	private int countPassedSigsCGFEfficient() {
		int sigs = 0;
		for (CallFrame fOuter : this.enterMonitorFrames) {
			sigs += this.isFrameReachableEfficient(fOuter);
			System.out.println(sigs);
		}
		return sigs;
	}

	// inner TODO
	private String formCSOuter(Vector<CallFrame> fOuter) {
		String call_stack_outer = "";
		for (CallFrame f : fOuter)
			call_stack_outer = call_stack_outer + f.toString() + ",";
		call_stack_outer = call_stack_outer.substring(0, call_stack_outer.length() - 1);
		return call_stack_outer;
	}

	// inner TODO
	private String formCSInner(Vector<CallFrame> fOuter, Vector<CallFrame> fInner) {
		String call_stack_inner = "";
		for (CallFrame f : fInner)
			call_stack_inner = call_stack_inner + f.toString() + ",";
		call_stack_inner = call_stack_inner.substring(0, call_stack_inner.length() - 1);
		return call_stack_inner;
	}

	// inner TODO
	private Vector<String> formRandomSignaturesWithInner(/* Vector<Vector<CallFrame>> stack, int numSigsToBeFormed */) {
		Vector<String> sigComponent = new Vector<String>();
		for (CallFrame fOuter : this.enterMonitorFrames) {
			if (this.frameReachabilityMap.get(fOuter) != null) {
				for (CallFrame fInner : this.frameReachabilityMap.get(fOuter))
					sigComponent.add(fOuter.toString() + "#" + fInner.toString());
			}
		}

		Vector<String> sigs = new Vector<String>();
		for (String component1 : sigComponent) {
			for (String component2 : sigComponent) {
				sigs.add("deadlock_template=1#" + component1 + ";1#" + component2);
			}
		}
		return sigs;
	}

	/**
	 * Forms numSigsToBeFormed random signatures for the given feasible call stacks
	 * 
	 * @param stack
	 * @param numSigsToBeFormed
	 * @return formed signatures list
	 */
	private Vector<String> formRandomSignatures(Vector<Vector<CallFrame>> stack, int numSigsToBeFormed) {
		System.out.println("FormRandomSigs");
		Vector<String> sigs = new Vector<String>();
		int random;
		Vector<CallFrame> f1;
		Vector<CallFrame> f2;
		String crtSig;
		while (sigs.size() < numSigsToBeFormed) {
			String call_stack_outer1 = "";
			random = (int) (r.nextDouble() * (stack.size() - 1));
			f1 = stack.get(random);
			for (CallFrame f : f1)
				call_stack_outer1 = call_stack_outer1 + f.toString() + ",";
			call_stack_outer1 = call_stack_outer1.substring(0, call_stack_outer1.length() - 1);
			String call_stack_outer2 = "";
			random = (int) (r.nextDouble() * (stack.size() - 1));
			f2 = stack.get(random);
			for (CallFrame f : f2)
				call_stack_outer2 = call_stack_outer2 + f.toString() + ",";
			call_stack_outer2 = call_stack_outer2.substring(0, call_stack_outer2.length() - 1);
			crtSig = "deadlock_template=1#" + call_stack_outer1 + ";1#" + call_stack_outer2;
			if (!sigs.contains(crtSig)) {
				sigs.add(crtSig);
			}
		}
		return sigs;
	}

	/**
	 * Reads the content of the file
	 * 
	 * @param filename
	 * @return the content
	 */
	public static Vector<String> readFile(String filename) {

		Vector<String> lines = new Vector<String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(filename));

			String line = "";
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return lines;
	}

	public static double getAverage(double[] times) {
		double sum = 0;
		for (double t : times)
			sum += t;
		return (double) (sum / times.length);
	}

	public static double getStd(double[] times) {
		double average = getAverage(times);
		double sum = 0;
		for (double t : times)
			sum = sum + Math.pow(t - average, 2);
		return Math.sqrt(sum / times.length);
	}

	/**
	 * Creates numSigsToBeChecked signatures for each depth in the given depth range and evaluates them, prints the results to the given
	 * files
	 * 
	 * @param classesPaths
	 * @param fInfo
	 * @param fSigs
	 * @param fPassedSigs
	 * @param depthStart
	 * @param depthEnd
	 */
	public void evaluate(String[] classesPaths, String fInfo, String fSigs, String fPassedSigs, int depthStart, int depthEnd,
			int numSigsToBeChecked, boolean checkInner, boolean fromFile) {
		this.enterMonitorFrames.clear();
		// System.out.println("Evaluate");
		// Count all the classes
		if (fromFile)
			this.getClasses(classesPaths[0], classesPaths[1]);
		else
			this.getClasses(classesPaths);

		try {
			BufferedWriter outFInfo = new BufferedWriter(new FileWriter(fInfo));
			// Count all the methods
			outFInfo.write("Number of methods: " + this.getMethodCount() + "\n");

			// Get the methods that has EnterMonitor and create a CallFrame for them
			this.getEnterMonitorFrames();
			outFInfo.write("Number of enter monitor frames: " + this.enterMonitorFrames.size() + "\n");
			this.createLockEvents();

			Vector<Vector<CallFrame>> stacks;
			for (int depth = depthStart; depth <= depthEnd; depth++) {
				Vector<HashMap<SootMethod, Integer>> maps_nstacks = new Vector<HashMap<SootMethod, Integer>>(depth);
				for (int d = 0; d < depth; d++)
					maps_nstacks.add(new HashMap<SootMethod, Integer>(65536));
				stacks = new Vector<Vector<CallFrame>>();
				int nStacksCGF = 0;
				for (CallFrame crtFrame : this.enterMonitorFrames) {
					Vector<CallFrame> stack = new Vector<CallFrame>();
					stack.add(crtFrame);
					if (stacks.size() < 1000)
						this.getCallStacks(crtFrame, stack, 1, depth, stacks, 1000 - stacks.size());
					nStacksCGF += this.countCallStacks(maps_nstacks, crtFrame.method, 1, depth);
					System.out.println(nStacksCGF + " stacks of depth " + depth + " pass the CG filter so far");
				}
				outFInfo.write("\n#stacks of depth " + depth + " = " + nStacksCGF);
				Vector<String> sigs;
				if (!checkInner) {
					if (numSigsToBeChecked != 0)
						sigs = this.formRandomSignatures(stacks, numSigsToBeChecked);
					else
						sigs = this.formSignatures(stacks);
					// writes the currently chosen random signatures into a file
					BufferedWriter outFSigs = new BufferedWriter(new FileWriter(fSigs + depth));
					for (String s : sigs)
						outFSigs.write(s + "\n");
					outFSigs.flush();
					outFSigs.close();

					this.validate(sigs, classesPaths, fPassedSigs, depth, outFInfo, checkInner);
				} else {
					System.out.println("analysis with inner call stacks");
					int count = this.countPassedSigsCGFEfficient();
					System.out.println("\n#stacks that can pass cgf= " + count);
					outFInfo.write("\n#stacks that can pass cgf= " + count);
					// count = this.countPassedSigsCFGF();
					// System.out.println("\n#sigs that can pass cfgf= " + count);
					// outFInfo.write("\n#sigs that can pass cfgf= " + count);
					// outFInfo.write("\n#sigs that can pass cgf= " + this.formSignaturesWithInnerCS(stacks));
					sigs = this.formRandomSignaturesWithInner();
					System.out.println("sigs formed");
					// writes the currently chosen random signatures into a file
					BufferedWriter outFSigs = new BufferedWriter(new FileWriter(fSigs + depth));
					for (String s : sigs)
						outFSigs.write(s + "\n");
					outFSigs.flush();
					outFSigs.close();
					System.out.println("number of sigs: " + sigs.size());
					this.validate(sigs, classesPaths, fPassedSigs, depth, outFInfo, checkInner);
				}
			}
			outFInfo.flush();
			outFInfo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks the signatures from the given file
	 * 
	 * @param classesPaths
	 * @param fInfo
	 * @param fSigs
	 * @param fPassedSigs
	 */
	public void evaluate(String[] classesPaths, String fInfo, String fSigs, String fPassedSigs, boolean checkInner) {
		this.getClasses(classesPaths);
		try {
			BufferedWriter outFInfo = new BufferedWriter(new FileWriter(fInfo));
			Vector<String> sigs = Evaluation.readFile(fSigs);
			this.validate(sigs, classesPaths, fPassedSigs, 0, outFInfo, checkInner);
			outFInfo.flush();
			outFInfo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void evaluateRandom(String[] classesPaths, String fSigs, String fInfo, String fPassedSigs, int depthStart, int depthEnd,
			boolean checkInner) {
		this.getClasses(classesPaths);

		Vector<Vector<Vector<CallFrame>>> allStacks = new Vector<Vector<Vector<CallFrame>>>();
		Vector<Vector<CallFrame>> stacks;

		for (int depth = depthStart; depth <= depthEnd; depth++) {
			stacks = new Vector<Vector<CallFrame>>();
			for (int i = 0; i < 10000/*
									 * this should be calibrated, but it's enough for 1000 sigs
									 */; i++) {
				Vector<CallFrame> stack = new Vector<CallFrame>();
				this.getRandomCallStacks(stack, depth, stacks);
			}
			int size = 0;
			for (Vector<CallFrame> s : stacks) {
				size += s.size();
			}

			allStacks.add(stacks);
		}

		Vector<String> sigs = new Vector<String>();
		for (int i = 0; i <= depthEnd - depthStart; i++)
			sigs.addAll(this.formRandomSignatures(allStacks.get(i)));

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fSigs));
			for (String s : sigs)
				out.write(s + "\n");
			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		this.evaluate(classesPaths, fInfo, fSigs, fPassedSigs, checkInner);
	}

	/**
	 * Performs the validation part of the evaluation
	 * 
	 * @param sigs
	 * @param classesPaths
	 * @param fPassedSigs
	 * @param depth
	 * @param outFInfo
	 */
	private void validate(Vector<String> sigs, String[] classesPaths, String fPassedSigs, int depth, BufferedWriter outFInfo,
			boolean checkInner) {
		Vector<Signature> sigsToBeChecked = new Vector<Signature>();
		for (String s : sigs) {
			// System.out.println(s);
			sigsToBeChecked.add(new Signature(s));
		}

		// System.out.println("hello");
		// read properties
		// Properties.read();

		// start validation
		IStaticAnalyzer sa = new StaticAnalysis(classesPaths, sigsToBeChecked, checkInner);
		Validator validator = new Validator();
		validator.registerStaticAnalyzer(sa);

		long startTime;
		long endTime;
		double[] timeForEachSig = new double[sigs.size() + 2];
		int count = -1;
		try {
			BufferedWriter outFPassedSigs = new BufferedWriter(new FileWriter(fPassedSigs + depth));
			for (Signature sig : sigsToBeChecked) {
				System.out.println(++count);
				startTime = System.nanoTime();
				StaticAnswer answer = validator.staticAnalyzer.checkSignature(sig);
				endTime = System.nanoTime();
				if (answer.isValid()) {
					// System.out.println("Signature valid\n");
					outFPassedSigs.write(sigs.get(count) + "\n");
				} else {
					// System.out.println("Signature invalid\n");
					// outFPassedSigs.write(sigs.get(count) + "\n");
				}
				timeForEachSig[count] = (double) ((endTime - startTime) / 1000000);
			}
			timeForEachSig[count + 1] = Evaluation.getAverage(timeForEachSig);
			timeForEachSig[count + 2] = Evaluation.getStd(timeForEachSig);
			outFInfo.write("\nDepth " + depth);
			outFInfo.write("\nAverage : " + timeForEachSig[count + 1]);
			outFInfo.write("\nSTD : " + timeForEachSig[count + 2]);
			outFPassedSigs.flush();
			outFPassedSigs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks the signatures from the given file
	 * 
	 * @param classesPaths
	 * @param fSigs
	 */
	public void evaluate(String[] classesPaths, String fSigs) {
		//this.getClasses(classesPaths);
		Vector<String> sigs = Evaluation.readFile(fSigs);
		this.validate(sigs, classesPaths);
	}

	/**
	 * Validates the signatures in sigs
	 * 
	 * @param sigs
	 * @param classesPaths
	 */
	private void validate(Vector<String> sigs, String[] classesPaths) {
		Vector<Signature> sigsToBeChecked = new Vector<Signature>();
		for (String s : sigs)
			sigsToBeChecked.add(new Signature(s));

		// start validation
		IStaticAnalyzer sa = new StaticAnalysis(classesPaths, sigsToBeChecked, false);
		Validator validator = new Validator();
		validator.registerStaticAnalyzer(sa);

		int count = -1;
		try {
			for (Signature sig : sigsToBeChecked) {
				System.out.println("\n");
				System.out.println(sigs.get(++count));
				StaticAnswer answer = validator.staticAnalyzer.checkSignature(sig);
				if (answer.isValid())
					System.out.println("Signature valid\n");
				else
					System.out.println("Signature invalid\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		// Eval in general
		Evaluation eval = new Evaluation();
		eval.evaluate(new String[] {args[0]}, args[1]);
/*		
		// MySQL-JDBC ---------------------------------------------------------
		Evaluation evalMySQLJDBC = new Evaluation();
		// evalMySQLJDBC.evaluate(new String[] {
		// "/home/pinar/workspace/mysql-connector-java-5.0.0-beta/bin" },
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-USENIX",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-USENIX-SigsDepth",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-USENIX-SigsPassedDepth",
		// 1, 4, 1000, false, false);

		//evalMySQLJDBC.evaluate(new String[] { "/home/pinar/workspace/mysql-connector-java-5.0.0-beta/bin" },
		//		"/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-Inner",
		//		"/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-Inner-SigsDepth",
		//		"/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-Inner-SigsPassedDepth", 1, 1, 1000, true, false);
		
		// evalMySQLJDBC
		// .evaluate(
		// new String[] { "/home/pinar/workspace/mysql-connector-java-5.0.0-beta/bin" },
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQLSigs",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQLSigsPassed");

		// Active-MQ ----------------------------------------------------------
		Evaluation evalActiveMQ = new Evaluation();
		// evalActiveMQ.evaluate(new String[] {
		// "/home/pinar/workspace/ActiveMQ-4.0-M4/activemq-core/target/classes" },
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/ActiveMQ",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/ActiveMQ-SigsDepth",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/ActiveMQ-SigsPassedDepth",
		// 1, 4, 1000);

		// Limewire -----------------------------------------------------------
		Evaluation evalLimewire = new Evaluation();
		// evalLimewire.evaluate(new String[] { "/home/pinar/workspace/limewire/components" },
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire-SigsDepth",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire-SigsPassedDepth", 1, 4,
		// 1000, false, false);

		// evalLimewire.evaluate(new String[] {"/home/pinar/workspace/limewire/components"},
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire-Reject-CFG",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/RandomSigs-Limewire",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire-SigsPassedDepthD",
		// false);

		// evalLimewire.evaluate(new String[] { "/home/pinar/Desktop/paths-limewire",
		// "/home/pinar/workspace/limewire/" },
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire-SigsDepth",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/Limewire-SigsPassedDepth",
		// 1, 4, 1000, false, true);

		// Vuze -----------------------------------------------------------
		Evaluation evalVuze = new Evaluation();
		// evalVuze.evaluate(new String[] {"/home/pinar/Work/vuze/Azureus2"},
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/vuze",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/vuze-SigsDepth",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/vuze-SigsPassedDepth",
		// 1, 4, 1000, false, false);

		// Microbenchmark -----------------------------------------------------
		Evaluation evalMicrobenchmark = new Evaluation();
		
		 * for (int i = 10; i < 50; i++) { evalMicrobenchmark.evaluate(new String[] {
		 * "/home/pinar/workspace/DimmunixVaccineMicrobenchmark/bin", "A" + i },
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkA" + i,
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkA" + i + "-SigsDepth",
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkA" + i + "-SigsPassedDepth", 3, 3, 1000, false, false); }
		 * 
		 * for (int i = 1; i <= 10; i++) { evalMicrobenchmark.evaluate(new String[] {
		 * "/home/pinar/workspace/DimmunixVaccineMicrobenchmark/bin", "B" + i },
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkB" + i,
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkB" + i + "-SigsDepth",
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkB" + i + "-SigsPassedDepth", 3, 3, 1000, false, false); }
		 * 
		 * for (int i = 26; i < 50; i++) { evalMicrobenchmark.evaluate(new String[] {
		 * "/home/pinar/workspace/DimmunixVaccineMicrobenchmark/bin", "D" + i },
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkD" + i,
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkD" + i + "-SigsDepth",
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkD" + i + "-SigsPassedDepth", (i + 1), (i + 1), 1, false,
		 * false); }
		 * 
		 * for (int i = 2; i < 10; i++) { evalMicrobenchmark.evaluate(new String[] {
		 * "/home/pinar/workspace/DimmunixVaccineMicrobenchmark/bin", "E" + i },
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkE" + i,
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkE" + i + "-SigsDepth",
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkE" + i + "-SigsPassedDepth", 3, 3, 10, false, false); }
		 * 
		 * for (int i = 0; i < 5; i++) { evalMicrobenchmark.evaluate(new String[] {
		 * "/home/pinar/workspace/DimmunixVaccineMicrobenchmark/bin", "F" + i },
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkF" + i,
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkF" + i + "-SigsDepth",
		 * "/home/pinar/workspace/dimmunixVaccine/src/signatures/MicrobenchmarkF" + i + "-SigsPassedDepth", 1, 1, 1000, false, false); }
		 

		// Random Evaluation --------------------------------------------------
		Evaluation randEval = new Evaluation();
		// randEval.evaluateRandom(new String[] { "/home/pinar/workspace/mysql-connector-java-5.0.0-beta/bin" },
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-USENIX-SigsRandom",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-USENIX-Random",
		// "/home/pinar/workspace/dimmunixVaccine/src/signatures/JDBCMySQL-USENIX-SigsPassedRandom", 1, 4, false);
*/	}
}