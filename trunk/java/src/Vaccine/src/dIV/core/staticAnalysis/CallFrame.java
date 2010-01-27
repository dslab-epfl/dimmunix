package dIV.core.staticAnalysis;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;

/**
 * Keeps a statement and its method
 * 
 * @author pinar
 */
public class CallFrame {
	public SootMethod method;
	public Stmt stmt;
	public int lineNumber;

	public CallFrame(SootMethod method, Stmt stmt) {
		this.method = method;
		this.stmt = stmt;
	}

	public CallFrame(SootMethod m, int l) {
		method = m;
		lineNumber = l;
		stmt = null;
	}

	public String toString() {
		int line = 0;
		if (stmt != null) {
			LineNumberTag ltag = ((LineNumberTag) stmt.getTag("LineNumberTag"));
			if (ltag != null)
				line = ltag.getLineNumber();
		} else
			line = lineNumber;
		return method.getDeclaringClass().getName() + "." + method.getName() + "(unknown:" + line + ")";
	}

	public boolean equals(Object o) {
		if (o instanceof CallFrame) {
			CallFrame f = (CallFrame) o;
			return this.method == f.method && this.stmt == f.stmt && this.lineNumber == f.lineNumber;
		} else
			return false;
	}
}