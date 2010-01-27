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

package dIV.core.staticAnalysis;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;

/**
 * Keeps a statement and its method
 * 
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