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

import java.util.LinkedList;

import soot.jimple.Stmt;

/**
 * Class that describes the analyzed primitives for counting locks/unlocks
 * 
 */
public class AnalyzedInvokes {
	
	static LinkedList<String> analyzedEnter = new LinkedList<String>();
	static LinkedList<String> analyzedExit = new LinkedList<String>();
	
	public static boolean isAnalyzedEnter(Stmt stmt) {
		
		if(analyzedEnter.size() == 0) {
			analyzedEnter.add("java.util.concurrent.Semaphore: void acquire()");
			analyzedEnter.add("java.util.concurrent.locks.ReentrantLock: void lock()");
			analyzedEnter.add("java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock: void lock()");
			analyzedEnter.add("java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock: void lock()");
			analyzedEnter.add("java.util.concurrent.locks.Lock: void lock()");
		}	
		
		for(String s : analyzedEnter)
			if(stmt.toString().indexOf(s) != -1)
				return true;
	
		return false;
	}
	
	public static boolean isAnalyzedExit(Stmt stmt) {
		
		if(analyzedExit.size() == 0) {
			analyzedExit.add("java.util.concurrent.Semaphore: void release()");
			analyzedExit.add("java.util.concurrent.locks.ReentrantLock: void unlock()");
			analyzedExit.add("java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock: void unlock()");
			analyzedExit.add("java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock: void unlock()");
			analyzedExit.add("java.util.concurrent.locks.Lock: void unlock()");
		}	
		
		for(String s : analyzedExit)
			if(stmt.toString().indexOf(s) != -1)
				return true;
		
		return false;
	}
	
}
