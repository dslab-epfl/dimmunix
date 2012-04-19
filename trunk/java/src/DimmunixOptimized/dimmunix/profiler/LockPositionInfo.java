package dimmunix.profiler;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import communix.CallStack;
import communix.Frame;

import dimmunix.Vector;
import dimmunix.analysis.HashAnalysis;

public class LockPositionInfo {
	public Vector<StackTraceElement> pos;
	public volatile boolean nested = false;
	
	public LockPositionInfo(Vector<StackTraceElement> pos) {
		this.pos = pos;
	}
	
	public AtomicInteger nacquired = new AtomicInteger(0);
	public HashSet<LockInfo> lockObjects = new HashSet<LockInfo>();
	public HashSet<Thread> lockThreads = new HashSet<Thread>();
	
	public long tSync = 0;
	
	public String toString() {
		String s = "";
		for (StackTraceElement f: pos) {
			if (!s.equals("")) {
				s = s+ ",";
			}
			s = s+ f;
		}
		
		return s;
	}
	
	public CallStack getCommunixCallStack() {
		Vector<Frame> frames = new Vector<Frame>(); 
		for (StackTraceElement p: this.pos) {
			int h = HashAnalysis.instance.getHash(p.getClassName());
			frames.add(new Frame(p, h));
		}
		return new CallStack(frames);
	}
}
