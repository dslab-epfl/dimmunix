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
import soot.jimple.EnterMonitorStmt;
import soot.jimple.Stmt;

/**
 * Class for keeping information about a lock event; keeps the lock and type of
 * the event related to that lock (is it a acquire or release event)
 * 
 */
public class LockEvent {
	public SootMethod method;
	public Stmt stmt;
	public boolean isSyncMethod;

	public LockEvent(SootMethod method, Stmt stmt, boolean isSynchMethod) {
		this.method = method;
		this.stmt = stmt;
		this.isSyncMethod = isSynchMethod;
	}

	public boolean isAcquire(){
		return (this.stmt instanceof EnterMonitorStmt);
	}
	
	public void print() {
		System.out.println("--Lock Event--");
		System.out.println("----Method: " + this.method.getSignature());
		System.out.println("----Stmt: " + this.stmt.toString() + " line=" +this.stmt.getTag("LineNumberTag"));
	}
}