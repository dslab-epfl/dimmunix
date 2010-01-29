package dIV.core.staticAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;
import dIV.interf.IStaticAnalyzer;
import dIV.util.StaticAnswer;

/**
 * Class that performs the validation of a signature
 * 
 */
public class StaticAnalysis extends IStaticAnalyzer {

	/** signature to be validated */
	private Signature sig;

	/** */
	private CallGraph callGraph;

	/** Keeps the methods that can reach a EnterMonitor or ExitMonitor statement */
	private HashMap<String, SootMethod> methodsLockUnlock = new HashMap<String, SootMethod>();

	/** Keeps the possible lock/unlock sequences for each thread in the signature */
	private Vector<Vector<Vector<Vector<LockEvent>>>> LSeqsList = new Vector<Vector<Vector<Vector<LockEvent>>>>();

	/** Object to perform may alias analysis */
	private MayAliasAnalysis mayAliasAnalysis;

	/** All the loaded classes */
	private HashSet<String> classes = new HashSet<String>();

	/** Class-SubClasses */
	private HashMap<SootClass, Vector<SootClass>> subClasses = new HashMap<SootClass, Vector<SootClass>>();

	/** Class-SuperClasses */
	private HashMap<SootClass, Vector<SootClass>> superClasses = new HashMap<SootClass, Vector<SootClass>>();

	/** Cache that keeps the lock/unlock sequences for previously seen frames */
	private HashMap<String, Vector<Vector<LockEvent>>> LSeqsFrameMap = new HashMap<String, Vector<Vector<LockEvent>>>();

	/** MayAlias cache */
	private HashMap<Value, HashMap<Value, Boolean>> mayAliasMap = new HashMap<Value, HashMap<Value, Boolean>>();

	/** true if inner call stacks are considered, false otherwise */
	boolean innerMode;

	/**
	 * 
	 * @param sigs
	 *            list of signatures to be checked
	 */
	public StaticAnalysis(String[] paths, Collection<Signature> sigs, boolean checkInnerCS) {
		this.setupSoot();
		// load classes
		this.loadClasses(paths, sigs);
		this.buildCallGraph();
		this.collectLockUnlockMethods();
		this.mayAliasAnalysis = new MayAliasAnalysis(this.classes, null);
		this.innerMode = checkInnerCS;
	}

	/**
	 * Prepares interaction with soot
	 */
	private void setupSoot() {
		Options.v().set_whole_program(true);
		Options.v().set_via_shimple(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(true);
	}

	/**
	 * 
	 * @param paths
	 */
	private void loadClasses(String[] paths, Collection<Signature> sigs) {
		Vector<String> v_cls = new Vector<String>();
		for (String p : paths)
			v_cls.addAll(SourceLocator.v().getClassesUnder(p));

		SootClass cl;
		for (String c : v_cls) {
			classes.add(c);
			cl = Scene.v().loadClassAndSupport(c);
			cl.setApplicationClass();
		}

		
		for (Signature s : sigs)
			for (String c : s.getClasses()) {
				if (!classes.contains(c)) {
					classes.add(c);
					cl = Scene.v().loadClassAndSupport(c);
					cl.setApplicationClass();
				}
			}

		Scene.v().loadDynamicClasses();
	}

	/**
	 * Initializes control flow graph
	 */
	private void buildCallGraph() {
		CallGraphBuilder cgb = new CallGraphBuilder();
		cgb.build();
		this.callGraph = cgb.getCallGraph();
	}

	/**
	 * Gets the corresponding SootMethod for the method in f
	 * 
	 * @param f
	 *            frame that has the information about the SootMethod to be returned
	 * @return SootMethod object for the method in the given Frame f
	 * 
	 */
	SootMethod getMethod(Frame f) {
		SootClass c = null;
		if (!Scene.v().containsClass(f.getClassName())) {
			c = Scene.v().loadClassAndSupport(f.getClassName());
			c.setInScene(true);
			if (!c.isApplicationClass())
				c.setApplicationClass();
		} else
			c = Scene.v().getSootClass(f.getClassName());
		for (SootMethod m : c.getMethods()) {
			if (m.getName().equals(f.getMethod())) {
				for (Unit u : m.retrieveActiveBody().getUnits()) {
					int lTag = this.getLineNumber(u);
					if (lTag != -1 && lTag == f.getLine())
						return m;
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param stm
	 * @return the line number of the given statement stm, if it is null returns -1
	 */
	private int getLineNumber(Unit stm) {
		LineNumberTag l = (LineNumberTag) stm.getTag("LineNumberTag");
		if (l != null)
			return l.getLineNumber();
		return -1;
	}

	/**
	 * 
	 * @param ug
	 * @param lineNumber
	 * @return statement from ug having the corresponding line number (if there are more statements having the same line number, it returns
	 *         the first one being an InvokeExpr or if an InvokeExpr does not exist, the last statement with this line number
	 */
	private Stmt getStmt(UnitGraph ug, int lineNumber) {
		Stmt last = null;
		Stmt crtStmt;
		for (Unit u : ug) {
			crtStmt = (Stmt) u;
			if (this.getLineNumber(crtStmt) == lineNumber) {
				if (crtStmt.containsInvokeExpr() || crtStmt instanceof EnterMonitorStmt || crtStmt instanceof ExitMonitorStmt)
					return crtStmt;
				last = crtStmt;
			}
		}
		return last;
	}

	/**
	 * @param ug
	 * @param lineNumber
	 * @return statements from ug having the corresponding line number
	 */
	private List<Stmt> getStms(UnitGraph ug, int lineNumber) {
		List<Stmt> result = new LinkedList<Stmt>();
		for (Unit u : ug) {
			Stmt stmt = (Stmt) u;
			if (this.getLineNumber(stmt) == lineNumber) {
				result.add(stmt);
			}
		}
		return result;
	}

	/**
	 * 
	 * @param cls
	 * @return super classes and interfaces for the given class cls
	 */
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

	/**
	 * 
	 * @param cls
	 * @return subclasses for the given class cls
	 */
	private Vector<SootClass> getSubClasses(SootClass cls) {
		if (this.subClasses.containsKey(cls))
			return this.subClasses.get(cls);
		else {
			Vector<SootClass> subClasses = new Vector<SootClass>();
			Stack<SootClass> classesToBeProcessed = new Stack<SootClass>();

			SootClass cl;
			SootClass currentClass;
			classesToBeProcessed.add(cls);

			while (!classesToBeProcessed.empty()) {
				currentClass = classesToBeProcessed.pop();
				for (String c : this.classes) {
					cl = Scene.v().getSootClass(c);
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

	/**
	 * @param m
	 * @return the methods that have the same subsignature with the given method m
	 */

	private Vector<SootMethod> getMethodsWithSameSignature(SootMethod m) {
		Vector<SootMethod> methods = new Vector<SootMethod>();
		Vector<SootClass> classes = this.getSubClasses(m.getDeclaringClass());
		SootMethod method;
		for (SootClass cl : classes) {
			try {
				method = cl.getMethod(m.getSubSignature());
				if (method != null && method.isConcrete())
					methods.add(method);
			} catch (RuntimeException e) {
				continue;
			}
		}
		return methods;
	}

	/**
	 * @param mCalled
	 * @param mChecked
	 * @return says whether m is in calls; handles overridden methods and dynamic method binding
	 */
	private boolean callsMatch(SootMethod mCalled, SootMethod mChecked) {
		// the Soot subsignature of this method; used to refer to methods unambiguously
		if (mCalled == null || mChecked == null)
			return false;
		if (mCalled == mChecked)
			return true;
		else if (!mCalled.getSubSignature().equals(mChecked.getSubSignature()))
			return false;
		else {
			Vector<SootClass> classes = this.getSuperClasses(mChecked.getDeclaringClass());
			return classes.contains(mCalled.getDeclaringClass());
		}
	}

	/**
	 * Checks whether the synchronized objects from l1 and l2 may alias
	 * 
	 * @param l1
	 * @param l2
	 * @return true, if l1 and l2 may alias; false, otherwise
	 */
	private boolean mayAlias(LockEvent l1, LockEvent l2) {
		// System.out.println("-----------"); // **
		// l1.print(); // **
		// l2.print(); // **

		// May Alias Cache Optimization
		//try{
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
		} /*
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("exception in may alias");
			return false;
		} */
	}

	/**
	 * Keeps the already created StrongLocalMustAliasAnalysis objects for a method
	 */
	private HashMap<SootMethod, StrongLocalMustAliasAnalysis> methodAliasAnalyses = new HashMap<SootMethod, StrongLocalMustAliasAnalysis>();

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
		// System.out.println("-----------"); // **
		// l1.print(); // **
		// l2.print(); // **
		if ((l1.isSyncMethod && !l2.isSyncMethod) || (!l1.isSyncMethod && l2.isSyncMethod) || (l1.method != l2.method))
			return false;
		else if (l1.isSyncMethod && l2.isSyncMethod)
			return this.getLineNumber(l1.stmt) == this.getLineNumber(l2.stmt);
		else {
			StrongLocalMustAliasAnalysis mustAliasAnalysis = this.getAliasAnalysis(l1.method);
			return mustAliasAnalysis.mustAlias((Local) ((JExitMonitorStmt) l1.stmt).getOp(), l1.stmt, (Local) ((JEnterMonitorStmt) l2.stmt)
					.getOp(), l2.stmt);
		}
	}

	/**
	 * Puts the predecessor methods of m into methodsLockUnlock
	 * 
	 * @param m
	 *            method whose predecessors are traversed
	 * @param methodsChecked
	 *            keeps the already traversed methods, to prevent cycles
	 * 
	 */
	private void collectPredMethods(SootMethod m, HashMap<String, SootMethod> methodsChecked) {
		Iterator<Edge> mPredIterator = this.callGraph.edgesInto(m);
		SootMethod mPred = null;
		Vector<SootMethod> methods;
		while (mPredIterator.hasNext()) {
			mPred = mPredIterator.next().getSrc().method();
			methods = this.getMethodsWithSameSignature(m);
			methods.add(mPred);
			for (SootMethod crtMethod : methods) {
				if (!methodsChecked.containsKey(crtMethod.getSignature())) {
					this.methodsLockUnlock.put(crtMethod.getSignature(), crtMethod);
					// System.out.println(mPred.getSignature()); // **
					methodsChecked.put(crtMethod.getSignature(), crtMethod);
					collectPredMethods(crtMethod, methodsChecked);
				}
			}
			methods.clear();
		}
	}

	/**
	 * Puts the methods into methodsLockUnlock if a lock/unlock call is reachable from them.
	 * 
	 */
	private void collectLockUnlockMethods() {
		// methodsChecked; To not to look at the methods more than once
		HashMap<String, SootMethod> methodsChecked = new HashMap<String, SootMethod>();
		Chain<SootClass> classes = Scene.v().getApplicationClasses();
		Vector<SootMethod> methods;
		for (SootClass c : classes) {
			for (SootMethod m : c.getMethods()) {
				methods = this.getMethodsWithSameSignature(m);
				methods.add(m);
				for (SootMethod crtMethod : methods) {
					if (!(methodsChecked.containsKey(crtMethod.getSignature()) || crtMethod.isNative() || crtMethod.isAbstract())) {
						if (crtMethod.isSynchronized())
							this.methodsLockUnlock.put(crtMethod.getSignature(), crtMethod);
						else {
							for (Unit u : crtMethod.retrieveActiveBody().getUnits()) {
								Stmt stmt = (Stmt) u;
								if (stmt instanceof EnterMonitorStmt || stmt instanceof ExitMonitorStmt) {
									this.methodsLockUnlock.put(crtMethod.getSignature(), crtMethod);
									break;
								}
							}
						}
						this.collectPredMethods(crtMethod, methodsChecked);
					}
				}
				methods.clear();
			}
		}
	}

	/**
	 * Checks whether the call stacks in the signature can be matched with the call graph of the program.
	 * 
	 * Heuristic; checks if a lock/unlock call is reachable from the currently checked method, if not returns false.
	 * 
	 * @return true, if the call stacks in the signature is feasible; false, otherwise
	 * 
	 */
	private boolean checkCallStack() {
		Frame crtFrame = null;
		Frame nextFrame = null;
		SootMethod crtMethod = null;
		SootMethod nextMethod = null;
		List<Stmt> crtStmts = null;
		boolean matched = false;

		for (SigComponent sc : this.sig.components) {
			// checks if the last frame is a MonitorEnter
			crtFrame = sc.outer.stack.elementAt(0);
			// crtFrame.print(); // **
			crtMethod = this.getMethod(crtFrame);
			if (crtMethod == null) {
				// System.out.println("method null"); // **
				return false;
			}
			// System.out.println(crtMethod.getSignature()); // **
			List<Stmt> lastStmts = this.getStms(new BriefUnitGraph(crtMethod.getActiveBody()), crtFrame.getLine());
			boolean isEnterMonitor = false;
			Vector<SootMethod> methods;
			for (Stmt s : lastStmts) {
				// System.out.println(s); // **
				if (s instanceof EnterMonitorStmt)
					isEnterMonitor = true;
				else if (s.containsInvokeExpr()) {
					methods = this.getMethodsWithSameSignature(s.getInvokeExpr().getMethod());
					methods.add(s.getInvokeExpr().getMethod());
					for (SootMethod m : methods) {
						// System.out.println(m.getSignature()); // **
						if (m.isSynchronized()) {
							isEnterMonitor = true;
							break;
						}
					}
				}
				if (isEnterMonitor)
					break;
			}
			if (!isEnterMonitor) {
				System.out.println("--not enter monitor--"); // **
				return false;
			}
			// checks if the nextFrame is reachable from the crtFrame for each
			// frame in the stack
			for (int i = sc.outer.getSize() - 1; i > 0; i--) {
				crtFrame = sc.outer.stack.elementAt(i);
				// crtFrame.print(); // **
				nextFrame = sc.outer.stack.elementAt(i - 1);
				crtMethod = this.getMethod(crtFrame);
				if (crtMethod == null)
					return false;
				// System.out.println(crtMethod.getSignature()); // **
				nextMethod = this.getMethod(nextFrame);
				crtStmts = this.getStms(new BriefUnitGraph(crtMethod.getActiveBody()), crtFrame.getLine());
				matched = false;
				for (Stmt s : crtStmts) {
					if (this.methodsLockUnlock.containsKey(crtMethod.getSignature()) && s.containsInvokeExpr()
							&& this.callsMatch(s.getInvokeExpr().getMethod(), nextMethod)) {
						matched = true;
						break;
					}
					// ** to check which one is violated
					/*
					 * if (!this.methodsLockUnlock.containsKey(crtMethod .getSignature()))
					 * System.out.println("--Not in methodsLockUnlock--"); // ** if (!s.containsInvokeExpr())
					 * System.out.println("--Not an invoke statement--"); // ** else { System.out.println("--Methods don't match--"); // **
					 * System.out.println(s.getInvokeExpr().getMethod() .getSignature()); System.out.println(nextMethod); }
					 */
				}
				if (!matched)
					return false;
			}
		}
		return true;
	}

	/**
	 * Collects all possible lock/unlock sequences till target statement
	 * 
	 * @param CS
	 *            call stack
	 * @param stmt
	 *            current statement to be executed
	 * @param LBranch
	 *            keeps the possible lock/unlock events for the current thread execution
	 * @param targetStmt
	 *            last statement to be executed
	 * @param crtMethod
	 *            method of the current statement
	 * @param stmts
	 *            statements in the current method
	 * @param nExecs
	 *            keeps how many times they are executed for each statement
	 * 
	 */
	private void collectLockSequences(Vector<CallFrame> CS, Stmt stmt, Vector<LockEvent> LBranch, Stmt targetStmt, SootMethod crtMethod,
			SootMethod initMethod, Chain<Unit> stmts, HashMap<Stmt, Integer> nExecs, Vector<Vector<LockEvent>> L, boolean isBranch) {
		if (stmt != null) {
			//System.out.println("Current Stmt: " + stmt.toString() + " line="
			// + this.getLineNumber(stmt)); // **
			// handle loops
			if(!isBranch) {
			Integer nExec = nExecs.remove(stmt);
			if (nExec != null)
				nExecs.put(stmt, nExec.intValue() + 1);
			else
				nExecs.put(stmt, 1);
			if (nExecs.get(stmt) > 2)
				return;
			}
			// handle end of the path
			if (stmt.equals(targetStmt)) {
				//System.out.println("s = starget"); // **
				// for EnterMonitorStmt
				if (targetStmt instanceof EnterMonitorStmt)
					LBranch.add(new LockEvent(crtMethod, stmt, false));
				else if (targetStmt.containsInvokeExpr() && targetStmt.getInvokeExpr().getMethod().isSynchronized()) {
					// SootMethod invkMethod = targetStmt.getInvokeExpr().getMethod();
					// Vector<SootMethod> methods = this
					// .getMethodsWithSameSignature(invkMethod);
					// for (SootMethod m : methods) {
					// if (m.isSynchronized())
					this.handleSyncMethods(targetStmt.getInvokeExpr().getMethod(), LBranch, true, this.getLineNumber(stmt));
					// }
				}
			}
			// handle lock/unlock operations
			else if (stmt instanceof EnterMonitorStmt || stmt instanceof ExitMonitorStmt) {
				//System.out.println("s = lock(x) or s = unlock(x)"); // **
				LBranch.add(new LockEvent(crtMethod, stmt, false));
				this.collectLockSequences(CS, (Stmt) stmts.getSuccOf(stmt), LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
			}
			// handle if conditions
			else if (stmt instanceof IfStmt) {
			//	System.out.println("s = if"); // **
				Vector<LockEvent> LBranchFalse = (Vector<LockEvent>) LBranch.clone();
				// handle if true
				this.collectLockSequences(CS, (Stmt) stmts.getSuccOf(stmt), LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
				// handle if false
				Stmt stmtFalse = ((IfStmt) stmt).getTarget();
				if (crtMethod != initMethod || (targetStmt != null && this.getLineNumber(stmtFalse) <= this.getLineNumber(targetStmt))) {
					this.collectLockSequences(CS, stmtFalse, LBranchFalse, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
					if (LBranchFalse.size() > 0) {
						//System.out.println("Branching...");
						L.add(LBranchFalse);
					}
				}
			}
			// handle switch
			else if (stmt instanceof TableSwitchStmt) {
				//System.out.println("s = switch"); // **
				List<Unit> targetStmts = ((TableSwitchStmt) stmt).getTargets();
				Vector<LockEvent> LBranchSwitch;
				Stmt crtTargetStmt;
				for (int i = 0; i < targetStmts.size() - 1; i++) {
					crtTargetStmt = (Stmt) targetStmts.get(i);
					LBranchSwitch = (Vector<LockEvent>) LBranch.clone();
					if (crtMethod != initMethod
							|| (targetStmt != null && this.getLineNumber(crtTargetStmt) <= this.getLineNumber(targetStmt))) {
						this.collectLockSequences(CS, crtTargetStmt, LBranchSwitch, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
						if (LBranchSwitch.size() > 0) {
							//System.out.println("Branching...");
							L.add(LBranchSwitch);
						}
					}
				}
				crtTargetStmt = (Stmt) targetStmts.get(targetStmts.size() - 1);
				if (crtMethod != initMethod || (targetStmt != null && this.getLineNumber(crtTargetStmt) <= this.getLineNumber(targetStmt))) {
					this.collectLockSequences(CS, crtTargetStmt, LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
				}
			}
			// handle goto
			else if (stmt instanceof GotoStmt) {
				//System.out.println("s = goto"); // **
				Stmt stmtGoto = (Stmt) ((GotoStmt) stmt).getTarget();
				this.collectLockSequences(CS, stmtGoto, LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
			}
			// handle return
			else if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
				//System.out.println("s = return"); // **
				if (CS.size() != 0) {
					CallFrame topFrame = CS.remove(CS.size() - 1);
					Chain<Unit> stmtsReturned = topFrame.method.getActiveBody().getUnits();
					this.collectLockSequences(CS, (Stmt) stmtsReturned.getSuccOf(topFrame.stmt), LBranch, targetStmt, topFrame.method,
							initMethod, stmtsReturned, nExecs, L, false);
				}
			}
			// handle function call
			else if (stmt.containsInvokeExpr()) {
				//System.out.println("s = call"); // **
				SootMethod calledMethod = stmt.getInvokeExpr().getMethod();
				if (!this.methodsLockUnlock.containsKey(calledMethod.getSignature()))
					this.collectLockSequences(CS, (Stmt) stmts.getSuccOf(stmt), LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs,
							L, false);
				else {
					//System.out.println("lock/unlock call");
					// to handle inheritance
					Vector<SootMethod> mWithSameSignature = this.getMethodsWithSameSignature(calledMethod);
					for (SootMethod m : mWithSameSignature) {
						if (!(m.isAbstract() || m.isNative())) {
							Vector<LockEvent> LBranchCall = (Vector<LockEvent>) LBranch.clone();
							this.handleSyncMethods(m, LBranchCall, true, this.getLineNumber(stmt));
							Chain<Unit> stmtsCalled = m.retrieveActiveBody().getUnits();
							CS.add(new CallFrame(crtMethod, stmt));
							this.collectLockSequences(CS, (Stmt) stmtsCalled.getFirst(), LBranchCall, targetStmt, m, initMethod,
									stmtsCalled, nExecs, L, true);
							this.handleSyncMethods(m, LBranchCall, false, this.getLineNumber(stmt));
							if (LBranchCall.size() > 0) {
								//System.out.println("Branching...");
								L.add(LBranchCall);
							}
						}
					}
					//
					if (!(calledMethod.isAbstract() || calledMethod.isNative())) {
						this.handleSyncMethods(calledMethod, LBranch, true, this.getLineNumber(stmt));
						Chain<Unit> stmtsCalled = calledMethod.retrieveActiveBody().getUnits();
						CS.add(new CallFrame(crtMethod, stmt));
						this.collectLockSequences(CS, (Stmt) stmtsCalled.getFirst(), LBranch, targetStmt, calledMethod, initMethod,
								stmtsCalled, nExecs, L, false);
						this.handleSyncMethods(calledMethod, LBranch, false, this.getLineNumber(stmt));
					} /*else this.collectLockSequences(CS, (Stmt) stmts.getSuccOf(stmt), LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs,
							L, false); */
				}
			} else if (stmts.getSuccOf(stmt) != null) {
				this.collectLockSequences(CS, (Stmt) stmts.getSuccOf(stmt), LBranch, targetStmt, crtMethod, initMethod, stmts, nExecs, L, false);
			}
		}
	}

	/**
	 * If m is a synchronized method creates the corresponding EnterMonitorStmts (if isEnterMonitor true) or ExitMonitorStmts for the object
	 * that method m is called and all the parameters that are passed to method m. Then, adds all of these EnterMonitorStmts or
	 * ExitMonitorStmts to LBranch.
	 * 
	 * If m is not a synchronized method does nothing.
	 * 
	 * @param m
	 * @param LBranch
	 */
	private void handleSyncMethods(SootMethod m, Vector<LockEvent> LBranch, boolean isEnterMonitor, int lineNumber) {
		if (m.isSynchronized()) {
			Value v;
			try {
				v = m.retrieveActiveBody().getThisLocal();
			} catch (RuntimeException e) {
				return;
			}
			Stmt s;
			if (isEnterMonitor) {
				s = new JEnterMonitorStmt(v);
				s.addTag(new LineNumberTag(lineNumber));
				LBranch.add(new LockEvent(m, s, true));
			} else {
				s = new JExitMonitorStmt(v);
				s.addTag(new LineNumberTag(lineNumber));
				LBranch.add(new LockEvent(m, s, true));
			}
			//System.out.println("synch method: ");
			//LBranch.get(LBranch.size()-1).print();
		}
	}

	/**
	 * Removes the empty branches from the list to avoid duplicate lock/unlock sequences
	 * 
	 * @param LSeqsList
	 * 
	 */
	private void removeEmptyBranches() {
		// remove empty branches
		for (Vector<Vector<Vector<LockEvent>>> thread : this.LSeqsList) {
			for (Vector<Vector<LockEvent>> frame : thread) {
				for (int i = 0; i < frame.size();) {
					if (frame.get(i).size() == 0)
						frame.remove(i);
					else
						i++;
				}
			}
		}
		// remove empty frames
		for (Vector<Vector<Vector<LockEvent>>> thread : this.LSeqsList) {
			for (int i = 0; i < thread.size();) {
				if (thread.get(i).size() == 0)
					thread.remove(i);
				else
					i++;
			}
		}
	}

	/**
	 * Prints the LSeqsList for debugging purposes
	 * 
	 * @param LSeqsList
	 * 
	 */
	private void printLSeqsList(Vector<Vector<Vector<Vector<LockEvent>>>> LSeqsList) {
		System.out.println("Printing LSeqsList after collectLockSequences----------------");
		for (Vector<Vector<Vector<LockEvent>>> LSeqs : LSeqsList) {
			System.out.println("---------Thread---------");
			for (Vector<Vector<LockEvent>> L : LSeqs) {
				System.out.println("-------Frame-------");
				for (Vector<LockEvent> LBranch : L) {
					System.out.println("----Branch----");
					for (LockEvent e : LBranch) {
						System.out.println("--Event--");
						e.print();
					}
				}
			}
		}
	}

	/**
	 * Checks if there are lock inversions in LSeqsList
	 * 
	 * @return true, if a lock inversion detected; false, otherwise
	 * 
	 */
	private boolean hasInversion() {

		this.removeEmptyBranches();
		
		//this.printLSeqsList(this.LSeqsList);

		InversionInstance[] outerLocks = new InversionInstance[this.LSeqsList.size()];
		InversionInstance[] innerLocks = new InversionInstance[this.LSeqsList.size()];
		Vector<Vector<Vector<LockEvent>>> t1;
		Vector<Vector<Vector<LockEvent>>> t2;
		Vector<Vector<LockEvent>> f1;
		Vector<Vector<LockEvent>> f2;
		Vector<LockEvent> b1;
		Vector<LockEvent> b2;
		LockEvent l1;
		LockEvent l2;
		int outerFIndex = 0;
		int outerBIndex = 0;
		int outerLIndex = 1;
		boolean finished = false;

		for (int tIndex = 0; tIndex < this.LSeqsList.size(); tIndex++) {

			t1 = this.LSeqsList.get(tIndex);
			t2 = this.LSeqsList.get((tIndex + 1) % this.LSeqsList.size());
			finished = false;

			for (int f1Index = outerFIndex; f1Index < t1.size() && !finished; f1Index++) {

				f1 = t1.get(f1Index);

				for (int f2Index = 0; f2Index < t2.size() && !finished; f2Index++) {

					f2 = t2.get(f2Index);

					for (int b1Index = outerBIndex; b1Index < f1.size() && !finished; b1Index++) {

						b1 = f1.get(b1Index);

						for (int b2Index = 0; b2Index < f2.size() && !finished; b2Index++) {

							b2 = f2.get(b2Index);

							for (int l1Index = outerLIndex; l1Index < b1.size() && !finished; l1Index++) {

								l1 = b1.get(l1Index);

								for (int l2Index = 0; l2Index < b2.size() && !finished; l2Index++) {

									l2 = b2.get(l2Index);

									if (l1.isAcquire() && l2.isAcquire() && this.mayAlias(l1, l2)) {
										outerLocks[(tIndex + 1) % this.LSeqsList.size()] = new InversionInstance((tIndex + 1)
												% this.LSeqsList.size(), f2Index, b2Index, l2Index, l2);
										innerLocks[tIndex] = new InversionInstance(tIndex, f1Index, b1Index, l1Index, l1);
										outerFIndex = f2Index;
										outerBIndex = b2Index;
										outerLIndex = l2Index;
										finished = true;
										// checks to reduce false positives
										if (tIndex != 0) {
											if ((innerLocks[tIndex].fIndex < outerLocks[tIndex].fIndex)
													|| (innerLocks[tIndex].fIndex == outerLocks[tIndex].fIndex && innerLocks[tIndex].bIndex != outerLocks[tIndex].bIndex)
													|| (innerLocks[tIndex].fIndex == outerLocks[tIndex].fIndex
															&& innerLocks[tIndex].bIndex == outerLocks[tIndex].bIndex && innerLocks[tIndex].lIndex > outerLocks[tIndex].lIndex))
												finished = false;
											else
												finished = this.mustAliasRoutine(outerLocks[tIndex], innerLocks[tIndex], tIndex);
										}
										if (finished && tIndex == LSeqsList.size()) {
											if ((innerLocks[0].fIndex < outerLocks[0].fIndex)
													|| (innerLocks[0].fIndex == outerLocks[0].fIndex && innerLocks[0].bIndex != outerLocks[0].bIndex)
													|| (innerLocks[0].fIndex == outerLocks[0].fIndex
															&& innerLocks[0].bIndex == outerLocks[0].bIndex && innerLocks[0].lIndex > outerLocks[0].lIndex))
												finished = false;
											else
												finished = this.mustAliasRoutine(outerLocks[0], innerLocks[0], 0);
										}
									}
								}
							}

							if (f1Index == 0)
								outerLIndex = 1;
							else
								outerLIndex = 0;
						}
					}

					outerBIndex = 0;

				}
			}

			if (!finished)
				return false;
		}
/*
		if (finished) {
			System.out.println("OuterLocks-------------------------------");
			for (InversionInstance i : outerLocks)
				i.l.print();
			System.out.println("InnerLocks-------------------------------");
			for (InversionInstance i : innerLocks)
				i.l.print();
		}
*/

		return finished;
	}

	/**
	 * Performs the three step mustAlias check when a lock inversion detected in hasInversion
	 * 
	 * @param outerL
	 * @param innerL
	 * @param tIndex
	 * @return true, if there is no unlock operation on outerL is performed before innerL is locked; false, otherwise
	 */
	private boolean mustAliasRoutine(InversionInstance outerL, InversionInstance innerL, int tIndex) {
		boolean finished = true;
		int lEnd;
		int lStart;
		LockEvent e;

		// for outerL.fIndex
		lStart = outerL.lIndex + 1;
		if (outerL.fIndex == innerL.fIndex)
			lEnd = innerL.lIndex;
		else
			lEnd = this.LSeqsList.get(tIndex).get(outerL.fIndex).get(outerL.bIndex).size();
		for (int lIndex = lStart; lIndex < lEnd; lIndex++) {
			e = this.LSeqsList.get(tIndex).get(outerL.fIndex).get(outerL.bIndex).get(lIndex);
			if (!e.isAcquire() && this.mustAlias(e, outerL.l))
				finished = false;
		}

		if (finished) {
			// for innerL.fIndex
			lEnd = innerL.lIndex;
			if (outerL.fIndex == innerL.fIndex)
				lStart = innerL.lIndex + 1;
			else
				lStart = 0;
			for (int lIndex = lStart; lIndex < lEnd; lIndex++) {
				e = this.LSeqsList.get(tIndex).get(innerL.fIndex).get(innerL.bIndex).get(lIndex);
				if (!e.isAcquire() && this.mustAlias(e, outerL.l))
					finished = false;
			}

			if (finished && innerL.fIndex > outerL.fIndex + 1) {
				// for intermediate frames
				boolean[] framesNoMustAlias = new boolean[innerL.fIndex - outerL.fIndex - 1];
				for (int i = 0; i < framesNoMustAlias.length; i++)
					framesNoMustAlias[i] = false;
				for (int fIndex = outerL.fIndex + 1; fIndex < innerL.fIndex; fIndex++) {
					for (int bIndex = 0; bIndex < this.LSeqsList.get(tIndex).get(fIndex).size()
							&& !framesNoMustAlias[fIndex - outerL.fIndex - 1]; bIndex++) {
						for (int lIndex = 0; lIndex < this.LSeqsList.get(tIndex).get(fIndex).get(bIndex).size() && finished; lIndex++) {
							e = this.LSeqsList.get(tIndex).get(fIndex).get(bIndex).get(lIndex);
							if (!e.isAcquire() && this.mustAlias(e, outerL.l))
								finished = false;
						}
						if (finished)
							framesNoMustAlias[fIndex - outerL.fIndex - 1] = true;
						else
							finished = true;
					}
				}
				finished = true;
				for (int i = 0; i < framesNoMustAlias.length; i++)
					if (!framesNoMustAlias[i]) {
						finished = false;
						break;
					}
			}
		}

		return finished;
	}

	/**
	 * Checks if a lock inversion is possible from the given signature. For this check, it first collects the possible lock/unlock
	 * sequences. Then checks for lock inversions.
	 */
	private boolean checkCFG() {
		// nExecs; to keep the number of executions for each statement
		HashMap<Stmt, Integer> nExecs = new HashMap<Stmt, Integer>();
		// 
		Vector<Vector<Vector<LockEvent>>> LSeqs;
		Vector<Vector<LockEvent>> L;
		Vector<LockEvent> LBranch;
		SootMethod crtMethod = null;
		Chain<Unit> stmts = null;
		Stmt firstStmt;
		Stmt targetStmt = null;
		Frame crtFrame;
		// CS for n+1th case
		Vector<CallFrame> CSLast = new Vector<CallFrame>();
		// Collect the sequence of lock/unlock operations for each thread
		for (SigComponent sc : this.sig.components) {
			//System.out.println("-------------------------------------"); // **
			LSeqs = new Vector<Vector<Vector<LockEvent>>>();
			for (int i = sc.outer.stack.size() - 1; i >= 0; i--) {
				crtFrame = sc.outer.stack.get(i);
				// System.out.print("--Frame" + (sc.outer.stack.size() - 1 - i)
				// + ":"); // **
				// crtFrame.print(); // **
				L = new Vector<Vector<LockEvent>>();
				LBranch = new Vector<LockEvent>();
				crtMethod = this.getMethod(crtFrame);
				if (crtMethod == null)
					return false;
				stmts = crtMethod.getActiveBody().getUnits();
				firstStmt = (Stmt) stmts.getFirst();
				targetStmt = this.getStmt(new BriefUnitGraph(crtMethod.getActiveBody()), crtFrame.getLine());
				// System.out.println("First stmt: " + firstStmt.toString()
				// + " line=" + this.getLineNumber(firstStmt)); // **
				// System.out.println("Target stmt: " + targetStmt.toString()
				// + " line=" + this.getLineNumber(targetStmt)); // **
				CSLast.add(new CallFrame(crtMethod, targetStmt));
				if (!this.LSeqsFrameMap.containsKey(crtFrame.getFormat())) {
					nExecs.clear();
					L.add(LBranch);
					collectLockSequences(new Vector<CallFrame>(), firstStmt, LBranch, targetStmt, crtMethod, crtMethod, stmts, nExecs, L, false);
					LSeqs.add(L);
					this.LSeqsFrameMap.put(crtFrame.getFormat(), L);
				} else
					LSeqs.add(this.LSeqsFrameMap.get(crtFrame.getFormat()));
			}
			// System.out.print("--Frame(n+1):"); // **
			// System.out.println("First stmt: "
			// + stmts.getSuccOf(targetStmt).toString() + " line="
			// + this.getLineNumber(stmts.getSuccOf(targetStmt))); // **
			// n+1th case
			L = new Vector<Vector<LockEvent>>();
			LBranch = new Vector<LockEvent>();
			nExecs.clear();
			CSLast.remove(CSLast.size() - 1);
			L.add(LBranch);
			// if (!targetStmt.containsInvokeExpr())
			collectLockSequences(CSLast, (Stmt) stmts.getSuccOf(targetStmt), LBranch, null, crtMethod, crtMethod, stmts, nExecs, L, false);
			/*
			 * else { Vector<SootMethod> methods = this.getMethodsWithSameSignature(targetStmt .getInvokeExpr().getMethod()); for (int i =
			 * 0; i < methods.size(); i++) { crtMethod = methods.get(i); if (crtMethod.isConcrete() && crtMethod.isSynchronized()) { stmts =
			 * crtMethod.retrieveActiveBody().getUnits(); firstStmt = (Stmt) stmts.getFirst(); collectLockSequences(CSLast, firstStmt,
			 * LBranch, null, crtMethod, crtMethod, stmts, nExecs, L); } } }
			 */
			LSeqs.add(L);
			this.LSeqsList.add(LSeqs);
		}
		return this.hasInversion();
	}

/*	
	// for inner
	private boolean hasEnterMonitor(List<Stmt> stmts) {
		boolean hasEnterMonitor = false;
		Vector<SootMethod> methods;
		for (Stmt s : stmts) {
			if (s instanceof EnterMonitorStmt)
				hasEnterMonitor = true;
			else if (s.containsInvokeExpr()) {
				methods = this.getMethodsWithSameSignature(s.getInvokeExpr().getMethod());
				methods.add(s.getInvokeExpr().getMethod());
				for (SootMethod m : methods) {
					if (m.isSynchronized()) {
						hasEnterMonitor = true;
						break;
					}
				}
			}
			if (hasEnterMonitor)
				break;
		}
		return hasEnterMonitor;
	}

	private boolean checkCallStackWithInnerCS() {
		Frame crtOuterFrame = null;
		Frame crtInnerFrame = null;
		Frame nextInnerFrame = null;
		SootMethod crtOuterMethod = null;
		SootMethod crtInnerMethod = null;
		SootMethod nextInnerMethod = null;
		List<Stmt> crtStmts = null;
		boolean matched = false;
		for (SigComponent sc : this.sig.components) {
			// checks whether the number of frames in the outer call stack are less than or equal to the ones in the inner call stack
			if (sc.outer.getSize() > sc.inner.getSize())
				return false;
			// checks if the last frame is a MonitorEnter
			crtOuterFrame = sc.outer.stack.elementAt(0);
			crtInnerFrame = sc.inner.stack.elementAt(0);
			crtOuterMethod = this.getMethod(crtOuterFrame);
			crtInnerMethod = this.getMethod(crtInnerFrame);
			if (crtOuterMethod == null || crtInnerMethod == null)
				return false;
			if (!this.hasEnterMonitor(this.getStms(new BriefUnitGraph(crtOuterMethod.getActiveBody()), crtOuterFrame.getLine()))
					|| !this.hasEnterMonitor(this.getStms(new BriefUnitGraph(crtInnerMethod.getActiveBody()), crtInnerFrame.getLine())))
				return false;
			// checks whether inner call stack has the same path with outer call stack till the outer lock
			for (int i = sc.outer.getSize() - 1, j = sc.inner.getSize() - 1; i > 0; i--, j--) {
				if (!sc.outer.stack.elementAt(i).match(sc.inner.stack.elementAt(j))) {
					System.out.println("inner CS has a diffrent path than outer CS"); // **
					return false;
				}
			}
			// checks if the nextFrame is reachable from the crtFrame for each frame in the stack
			for (int i = sc.inner.getSize() - 1; i > 0; i--) {
				crtInnerFrame = sc.outer.stack.elementAt(i);
				nextInnerFrame = sc.inner.stack.elementAt(i - 1);
				crtInnerMethod = this.getMethod(crtInnerFrame);
				nextInnerMethod = this.getMethod(nextInnerFrame);
				crtStmts = this.getStms(new BriefUnitGraph(crtInnerMethod.getActiveBody()), crtInnerFrame.getLine());
				matched = false;
				for (Stmt s : crtStmts) {
					if (this.methodsLockUnlock.containsKey(crtInnerMethod.getSignature()) && s.containsInvokeExpr()
							&& this.callsMatch(s.getInvokeExpr().getMethod(), nextInnerMethod)) {
						matched = true;
						break;
					}
				}
				if (!matched)
					return false;
			}
		}
		return true;
	}

	private boolean hasInversionWithInner(LockEvent[] outerLocks, LockEvent[] innerLocks) {
		boolean hasInversion = true;
		int numThreads = outerLocks.length;
		for (int i = 0; i < numThreads && hasInversion; i++) {
			if (!this.mayAlias(outerLocks[i], innerLocks[(i + 1) % numThreads]))
				hasInversion = false;
		}
		if (!hasInversion) {
			hasInversion = true;
			for (int i = 0; i < numThreads && hasInversion; i++) {
				if (!this.mayAlias(outerLocks[(i + 1) % numThreads], innerLocks[i]))
					hasInversion = false;
			}
		}
		return hasInversion;
	}

	private boolean checkCFGWithInner() {

		LockEvent[] outerLocks = new LockEvent[this.sig.components.size()];
		LockEvent[] innerLocks = new LockEvent[this.sig.components.size()];

		boolean found;
		int component = 0;
		for (SigComponent sc : this.sig.components) {
			found = false;
			Frame outerFrame = sc.outer.stack.elementAt(0);
			Frame innerFrame = sc.inner.stack.elementAt(sc.inner.getSize() - sc.outer.getSize());
			SootMethod outerMethod = this.getMethod(outerFrame);
			SootMethod innerMethod = this.getMethod(innerFrame);

			// checks whether innerFrame is in synch block of the outer frame
			BriefUnitGraph ugOuter = new BriefUnitGraph(outerMethod.getActiveBody());
			Stmt outerLockStmt = this.getStmt(ugOuter, outerFrame.getLine());
			Stmt innerStmt = this.getStmt(new BriefUnitGraph(innerMethod.getActiveBody()), innerFrame.getLine());
			Stmt crtStmt;
			if (outerLockStmt.containsInvokeExpr()) {
				// handle synch methods
				outerLocks[component] = new LockEvent(outerMethod, outerLockStmt, true);
				SootMethod synchMethod = outerLockStmt.getInvokeExpr().getMethod();
				for (Unit u : new BriefUnitGraph(synchMethod.getActiveBody()).getBody().getUnits()) {
					crtStmt = (Stmt) u;
					if (crtStmt == innerStmt) {
						found = true;
						break;
					}
				}
			} else {
				// handle synch blocks
				outerLocks[component] = new LockEvent(outerMethod, outerLockStmt, false);
				for (Unit u : ugOuter.getSuccsOf(outerLockStmt)) {
					crtStmt = (Stmt) u;
					if (crtStmt instanceof ExitMonitorStmt
							&& this.mustAlias(new LockEvent(outerMethod, crtStmt, false), outerLocks[component]))
						break;
					else if (crtStmt == innerStmt) {
						found = true;
						break;
					}
				}
			}

			if (!found)
				return false;

			// get inner enter monitor statement
			innerFrame = sc.inner.stack.elementAt(0);
			innerMethod = this.getMethod(innerFrame);
			Stmt innerLockStmt = this.getStmt(new BriefUnitGraph(innerMethod.getActiveBody()), innerFrame.getLine());
			if (innerLockStmt.containsInvokeExpr())
				innerLocks[component] = new LockEvent(innerMethod, innerLockStmt, true);
			else
				innerLocks[component] = new LockEvent(innerMethod, innerLockStmt, false);

			component++;
		}

		return this.hasInversionWithInner(outerLocks, innerLocks);
	}

	// end inner
*/
	/**
	 * Checks if the signature is ok
	 */
	private boolean check() {
	/*	if (this.innerMode) {
			if (!this.checkCallStackWithInnerCS()) {
				System.out.println("Call Stacks don't match!");
				return false;
			} else if (!this.checkCFGWithInner()) {
				System.out.println("No lock inversion from the control flow graph check!");
				return false;
			} else
				return true;
		} else { */
			if (!this.checkCallStack()) {
				System.out.println("Call Stacks don't match!");
				return false;
			} else if (!this.checkCFG()) {
				System.out.println("No lock inversion from the control flow graph check!");
				return false;
			} else
				return true;
	//	}
	}

	@Override
	public StaticAnswer checkSignature(Signature s) {
		this.LSeqsList = new Vector<Vector<Vector<Vector<LockEvent>>>>();
		this.sig = s;
		SootClass cl;
		for (String c : this.sig.getClasses()) {
			cl = Scene.v().getSootClass(c);
			this.getSubClasses(cl);
			this.getSuperClasses(cl);
		}
		boolean result = this.check();
		return new StaticAnswer(result);
	}
}