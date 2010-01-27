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

import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class representing a signature
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