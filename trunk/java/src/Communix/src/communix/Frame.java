package communix;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Frame {
	public final StackTraceElement frame;
	public final int hash;
	
	public Frame(StackTraceElement frame, int hash) {
		this.frame = frame;
		this.hash = hash;
	}

	public Frame(String f) throws Exception {
		try {
			StringTokenizer st = new StringTokenizer(f, "()");

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

			this.frame = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
			
			this.hash = Integer.parseInt(st.nextToken());			
		}
		catch (NoSuchElementException ex) {
			throw new Exception("failed to parse frame");
		}
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((frame == null) ? 0 : frame.hashCode());
		result = prime * result + hash;
		return result;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Frame) {
			Frame f = (Frame)obj;
			return this.frame.equals(f.frame) && this.hash == f.hash;
		}
		return false;
	}	
	
	public String toString() {
		return this.frame+ ""+ this.hash;
	}
}
