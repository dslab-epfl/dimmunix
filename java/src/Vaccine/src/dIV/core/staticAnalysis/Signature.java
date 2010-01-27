package dIV.core.staticAnalysis;

import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class representing a signature
 * 
 * @author cristina
 * 
 */
public class Signature {
	int noThreads; // number of threads
	Vector<SigComponent> components; // a component consists of depth and outer
										// call stack

	/**
	 * parses a Signature having the format
	 * deadlock_template=depth1#call_stack_outer1;depth2#call_stack_outer2
	 * 
	 * @param format
	 */
	public Signature(String format) {
		try {
			StringTokenizer st = new StringTokenizer(format, "=");
			if (st.countTokens() != 2)
				throw new SigFormatException();

			st.nextToken();
			String utilPart = st.nextToken(); // the part after '=' sign

			st = new StringTokenizer(utilPart, ";");
			this.noThreads = st.countTokens();

			// there should be at least two threads involved in a deadlock
			if (noThreads < 2)
				throw new SigFormatException();

			this.components = new Vector<SigComponent>(this.noThreads);

			for (int i = 0; i < this.noThreads; i++) {
				components.add(new SigComponent(st.nextToken()));
			}
		} catch (SigFormatException e) {
			System.err.println(e.toString());
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			System.err.println("Bag signature format");
			System.out.println("CAUSE:");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void print() {
		for (int i = 0; i < this.noThreads; i++) {
			System.out.println("thread " + i);
			this.components.get(i).print();
		}
	}

	public void reverse() {
		for (int i = 0; i < this.noThreads; i++) {
			this.components.get(i).reverse();
		}
	}

	/**
	 * @return the classes from the signature
	 */
	public HashSet<String> getClasses() {

		HashSet<String> result = new HashSet<String>();

		if (this.components == null)
			return null;

		for (SigComponent sc : this.components) {
			for (Frame f : sc.outer.stack) {
				String classname = f.getClassName();
				if (!result.contains(classname))
					result.add(classname);
			}
		}

		return result;
	}
}