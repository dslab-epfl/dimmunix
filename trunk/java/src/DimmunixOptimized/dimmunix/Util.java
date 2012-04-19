package dimmunix;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.StringTokenizer;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;

public class Util {
	public static int computeHash(byte[] classContent) {
		int prime = 31;
		int hash = 1;

		for (int i = 0; i < classContent.length; i++) {
			hash = hash* prime+ classContent[i];
		}

		return hash;
	}

	public static StackTraceElement parsePosition(String pos) {
		StringTokenizer st = new StringTokenizer(pos, "()");
		String tok1 = st.nextToken();
		String tok2 = st.nextToken();

		String declaringClass = tok1.substring(0, tok1.lastIndexOf('.'));
		String methodName = tok1.substring(tok1.lastIndexOf('.') + 1);
		StringTokenizer st2 = new StringTokenizer(tok2, ":");
		String fileName = st2.nextToken();
		int lineNumber = 1;
		if (st2.hasMoreTokens()) {
			lineNumber = Integer.parseInt(st2.nextToken());
		}
		else {
			fileName = null;
		}

		return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
	}
	
	public static <T> boolean intersect(HashSet<T> s1, HashSet<T> s2) {
		for (T o : s1) {
			if (s2.contains(o))
				return true;
		}
		return false;
	}
	
	public static int getLineNumber(Stmt s) {
		LineNumberTag line = (LineNumberTag) s.getTag("LineNumberTag");		
		if (line != null)
			return line.getLineNumber();
		return 0;
	}
	
	public static StackTraceElement getPosition(SootMethod m, Unit s) {
		LineNumberTag line = (LineNumberTag) s.getTag("LineNumberTag");
		SourceFileTag srcFile = (SourceFileTag) m.getDeclaringClass().getTag("SourceFileTag");

		if (line != null && srcFile != null) {
			StackTraceElement location = new StackTraceElement(m.getDeclaringClass().getName(), m.getName(), srcFile.getSourceFile(), line.getLineNumber());
			return location;
		}

		return null;
	}	
	
	public static ThreadGroup getRootThreadGroup() {
	    ThreadGroup tg = Thread.currentThread().getThreadGroup();
	    ThreadGroup ptg;
	    while ((ptg = tg.getParent()) != null)
	        tg = ptg;
	    return tg;
	}
	
	private static Thread[] threads = new Thread[1000];  
	
	public static Thread[] getAllThreads() {
	    final ThreadGroup root = getRootThreadGroup();
	    int nAlloc = threads.length;
	    int n = 0;
	    while (true) {
	        n = root.enumerate(threads, true);
	        if (n == nAlloc) {
	        	nAlloc *= 2;
	        	threads = new Thread[nAlloc];
	        }
	        else
	        	break;
	    } 
	    return java.util.Arrays.copyOf(threads, n);
	}
	
	public static Thread getThread(long id) {
	    Thread[] threads = getAllThreads();
	    for (Thread thread: threads)
	        if (thread.getId() == id)
	            return thread;
	    return null;
	}
	
	private static int pid = 0;
	
	public static int getPID() {
		if (pid == 0) {
			String procName = ManagementFactory.getRuntimeMXBean().getName();

			StringTokenizer stok = new StringTokenizer(procName, "@");
			pid = Integer.parseInt(stok.nextToken());
		}
		return pid;		
	}
}
