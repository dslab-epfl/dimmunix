/*
     Created by Saman A. Zonouz, Horatiu Jula, Pinar Tozun, Cristina Basescu, George Candea
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix Vaccination Framework.

     Dimmunix Vaccination Framework is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix Vaccination Framework is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix Vaccination Framework. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

package dIV.core.staticAnalysis;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Class representing a thread's component in a Signature
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