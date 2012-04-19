package communix;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

public class CallStack {
	public final List<Frame> frames;

	public CallStack(List<Frame> frames) {
		if (frames.isEmpty()) {
			throw new IllegalArgumentException("empty call stack");
		}
		this.frames = Collections.unmodifiableList(frames);
	}	
	
	public CallStack(String cs) throws Exception {
		try {
			this.frames = new Vector<Frame>();
			StringTokenizer st = new StringTokenizer(cs, ",");
			
			while (st.hasMoreTokens()) {
				this.frames.add(new Frame(st.nextToken()));
			}			
		}
		catch (NoSuchElementException ex) {
			throw new Exception("failed to parse call stack");
		}
		if (this.frames.isEmpty()) {
			throw new IllegalArgumentException("empty call stack");
		}
	}
	
	public CallStack merge(CallStack s) {
		Vector<Frame> commonFrames = new Vector<Frame>();
		
		if (!this.frames.get(0).equals(s.frames.get(0))) {
			//top frames must match
			return null;
		}
		
		//find longest common suffix
		for (int i = 0; i < Math.min(this.frames.size(), s.frames.size()); i++) {
			if (this.frames.get(i).equals(s.frames.get(i))) {
				commonFrames.add(this.frames.get(i));
			}	
			else {
				break;
			}
		}
		
		return new CallStack(commonFrames);
	}
	
	public int hashCode() {
		return frames.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof CallStack) {
			CallStack s = (CallStack)obj;
			return (this.frames.equals(s.frames));
		}
		return false;
	}
	
	public String toString() {
		String s = "";
		for (int i = 0; i < this.frames.size(); i++) {
			if (i > 0) {
				s += ",";
			}
			s += this.frames.get(i);
		}
		
		return s;
	}
}
