package dimmunix;

import java.util.StringTokenizer;

public class CallStack {
	Vector<StackTraceElement> frames;
	
	public CallStack(int depth) {
		this.frames = new Vector<StackTraceElement>(depth);
	}
	
	public CallStack(Vector<StackTraceElement> frames) {
		this.frames = frames;
	}

	public boolean equals(Object obj) {
		if (obj == this)
		    return true;
		if (obj == null || !(obj instanceof CallStack))
		    return false;

		CallStack s = (CallStack)obj;
		return this.frames.equals(s.frames);
	}

	public int hashCode() {
		return frames.hashCode();
	}

	public String toString() {
		return frames.toString();
	}
	
	public CallStack cloneStack() {
		return new CallStack(this.frames.cloneVector());
	}
	
	public int size() {
		return this.frames.size();
	}
	
	public StackTraceElement get(int i) {
		return this.frames.get(i);
	}
	
	public void add(StackTraceElement frame) {
		this.frames.add(frame);
	}

	public void add(String frame) {
		StringTokenizer st = new StringTokenizer(frame, "()");
		String tok1 = st.nextToken();
		String tok2 = st.nextToken();
		
		String declaringClass = tok1.substring(0, tok1.lastIndexOf('.'));
		String methodName = tok1.substring(tok1.lastIndexOf('.')+ 1);
		StringTokenizer st2 = new StringTokenizer(tok2, ":");
		String fileName = st2.nextToken();
		int lineNumber = 0;
		if (st2.hasMoreTokens())
			lineNumber = Integer.parseInt(st2.nextToken());
		
		this.frames.add(new StackTraceElement(declaringClass, methodName, fileName, lineNumber));
	}

	public void clear() {
		this.frames.clear();
	}
}
