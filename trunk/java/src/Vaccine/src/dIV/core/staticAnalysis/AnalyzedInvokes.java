package dIV.core.staticAnalysis;

import java.util.LinkedList;

import soot.jimple.Stmt;

/**
 * Class that describes the analyzed primitives for counting locks/unlocks
 * 
 * @author cristina
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
