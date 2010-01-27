package dIV.core.staticAnalysis;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Class representing a thread's component in a Signature
 * 
 * @author cristina
 * 
 */
public class SigComponent {
	int depth;
	CallStack outer;

	CallStack inner;

	/**
	 * parses a component having the format depth1#call_stack_outer1#call_stack_inner1
	 * 
	 * @param format
	 */
	public SigComponent(String format) throws NoSuchElementException, SigFormatException {

		if (format.compareTo("") == 0)
			throw new SigFormatException();

		StringTokenizer st = new StringTokenizer(format, "#");

		if (st.countTokens() != 3 && st.countTokens() != 2)
			throw new SigFormatException();

		this.depth = Integer.parseInt(st.nextToken());

		// parse outer call stack
		this.outer = new CallStack(st.nextToken());

		// parse inner call stack
		if (st.countTokens() != 0)
			this.inner = new CallStack(st.nextToken());
	}

	public void print() {
		System.out.println("depth = " + this.depth);
		System.out.println("outer:");
		this.outer.print();
		System.out.println("inner:");
		this.inner.print();
	}

	public void reverse() {
		this.outer.reverse();
	}
}