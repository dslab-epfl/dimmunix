package communix;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

public class Signature {
	public final List<CallStack> outerStacks;
	public final List<CallStack> innerStacks;
	public final int size;
	
	public Signature(List<CallStack> outerStacks, List<CallStack> innerStacks) {
		this.outerStacks = Collections.unmodifiableList(outerStacks);
		this.innerStacks = Collections.unmodifiableList(innerStacks);

		if (this.outerStacks.size() != this.innerStacks.size() || this.outerStacks.size() < 2) {
			throw new IllegalArgumentException("signature incomplete");
		}
		this.size = this.outerStacks.size();
	}	
	
	public Signature(String sig) throws Exception {
		try {
			StringTokenizer st = new StringTokenizer(sig, ";");
			this.outerStacks = new Vector<CallStack>();
			this.innerStacks = new Vector<CallStack>();
			
			while (st.hasMoreTokens()) {
				this.outerStacks.add(new CallStack(st.nextToken()));
				this.innerStacks.add(new CallStack(st.nextToken()));
			}
			
			if (this.outerStacks.size() < 2) {
				throw new IllegalArgumentException("signature incomplete");
			}
			
			this.size = this.outerStacks.size();			
		}
		catch (NoSuchElementException ex) {
			throw new Exception("failed to parse signature");			
		}
	}
	
	public Signature merge(Signature s) {
		if (this.size != s.size) {
			return null;
		}
		
		Vector<CallStack> outerCommonStacks = new Vector<CallStack>(); 
		Vector<CallStack> innerCommonStacks = new Vector<CallStack>(); 
		
		for (int i = 0; i < this.size; i++) {
			CallStack commonStack = this.outerStacks.get(i).merge(s.outerStacks.get(i));
			if (commonStack == null) {
				return null;
			}
			outerCommonStacks.add(commonStack);
			
			commonStack = this.innerStacks.get(i).merge(s.innerStacks.get(i));
			if (commonStack == null) {
				return null;
			}
			innerCommonStacks.add(commonStack);
		}
		
		return new Signature(outerCommonStacks, innerCommonStacks);
	}
	
	public String toString() {
		String s = "";
		for (int i = 0; i < this.size; i++) {
			if (i > 0) {
				s += ";";
			}
			s += this.outerStacks.get(i)+ ";"+ this.innerStacks.get(i);
		}
		
		return s;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Signature) {
			Signature s = (Signature)obj;			
			return this.size == s.size && this.outerStacks.equals(s.outerStacks) && this.innerStacks.equals(s.innerStacks);
		}
		return false;		
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime* result+ this.outerStacks.hashCode(); 
		result = prime* result+ this.innerStacks.hashCode();		
		return result;
	}
}
