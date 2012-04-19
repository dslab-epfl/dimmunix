package dimmunix.init;

import java.util.StringTokenizer;

import dimmunix.Util;
import dimmunix.Vector;

public class Signature {
	public final StackTraceElement lockPos;
	public final String className;
	
	private Vector<Thread> threads = new Vector<Thread>();
	private Thread initializerThread = null;
	private volatile boolean initialized = false;
	
	public Signature(String sigStr) {
		StringTokenizer stok = new StringTokenizer(sigStr, ",");
		this.lockPos = Util.parsePosition(stok.nextToken());
		this.className = stok.nextToken();
	}
	
	public Signature(StackTraceElement lockPos, String className) {
		this.lockPos = lockPos;
		this.className = className;
	}
	
	public String toString() {
		return this.lockPos+ ","+ this.className;
	}
	
	public int hashCode() {
		return this.lockPos.hashCode()^ this.className.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Signature) {
			Signature sig = (Signature)obj;			
			return this.lockPos.equals(sig.lockPos) && this.className.equals(sig.className);
		}
		return false;
	}
	
	public void enter(Thread t) {
		this.threads.add(t);
	}
	
	public void exit(Thread t) {
		this.threads.remove(t);
	}
	
	public void startInit(Thread t) {
		this.initializerThread = t;
	}
	
	public void stopInit() {
		this.initializerThread = null;
		this.initialized = true;
	}
	
	public boolean isInstantiated() {
		if (this.initializerThread == null) {
			return false;
		}
		
		for (Thread t: this.threads) {
			if (t != this.initializerThread) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isInitialized() {
		return this.initialized;
	}
}
