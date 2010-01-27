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

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class representing a call stack
 * 
 */
public class CallStack {
	Vector<Frame> stack;
	
	/**
	 * parses a call stack where having the format f_1(file_1:line_1),...,f_n(file_n:line_n)
	 * 
	 * @param format
	 */
	public CallStack (String format) throws NoSuchElementException, SigFormatException{
		if(format.compareTo("") == 0)
			throw new SigFormatException();
		
		String formatCopy = new String(format);
		int index = 0;
		int noCommas = 0;
		
		StringTokenizer st = new StringTokenizer(format, ",");
		
		// count commas in the string
		while(formatCopy.indexOf(",", index) != -1) {
			index = formatCopy.indexOf(",", index) + 1;
			noCommas++;
		}
		
		int noTokens = st.countTokens();
		if(noTokens != noCommas + 1)
			throw new SigFormatException();
		
		this.stack = new Vector<Frame>(noTokens);
		
		for(int i = 0 ; i < noTokens ; i++) {
			this.stack.add(new Frame(st.nextToken()));
		}
	}
	
	/**
	 * builds a new call stack having the frames in vector s
	 * @param s
	 */
	public CallStack (Vector<Frame> s) {
		stack = new Vector<Frame>(s.size());
		
		for(int i = 0 ; i < s.size() ; i++) {
			stack.add(s.get(i));
		}
	}
	
	/**
	 * builds a new call stack having the frames from the current call stack plus frame f
	 * @param s
	 * @return 
	 */
	public CallStack add (Frame f) {
		Vector<Frame> newStack = new Vector<Frame>(this.stack.size() + 1);
		
		for(int i = 0 ; i < this.stack.size() ; i++) {
			newStack.add(this.stack.get(i));
		}
		
		newStack.add(f);
		
		return new CallStack(newStack);
	}
	
	/**
	 * @return the size of the call stack
	 */
	public int getSize() {
		return stack.size();
	}
	
	public void print() {
		for(int i = 0 ; i < this.stack.size() ; i++) {
			this.stack.get(i).print();
		}
	}
	
	/**
	 * reverses the call stack
	 */
	public void reverse() {
		Collections.reverse(stack);
	}
	
	/**
	 * checks if the current callStack matches s in a way indicated by precise
	 * @param s
	 * @param precise - check if the call stacks are the same
	 */
	public boolean match (CallStack s, boolean precise) {
		
		if(!precise) {
			// check if s is large enough
			if(s.stack.size() < this.stack.size())
				return false;
		}
		else {
			// check if they have the same size
			if(s.stack.size() != this.stack.size())
				return false;
		}
		
		// check if there is a match
		for(int i = 0 ; i < stack.size() ; i++) {
			Frame crtFrame = stack.get(i);
			Frame frameToCompare = s.stack.get(i);
			
			if(!crtFrame.match(frameToCompare))
				return false;
		}
		
		return true;
	}
	
	/**
	 * @return the stack without the last frame
	 */
	public CallStack head() {
		Vector<Frame> result = new Vector<Frame>(this.getSize() - 1);
		
		for(int i = 0 ; i < this.getSize() - 1 ; i++) {
			result.add(this.stack.get(i));
		}
		
		return new CallStack(result);
	}
	
	/**
	 * @param f
	 * @return a CallStack that contains from the current call stack only frames from f further
	 * (without f)
	 */
	public CallStack suffix(Frame f) {
		boolean copy = false;	// says if it already encountered Frame f
		int size = 0;
		
		Vector<Frame> result = null;
		
		for(int i = 0 ; i < this.getSize(); i++) {
			if(copy)
				size++;
			if(!copy && this.stack.get(i).match(f))
				copy = true;
		}
		
		result = new Vector<Frame>(size);
		copy = false;
		
		for(int i = 0 ; i < this.getSize(); i++) {
			if(copy)
				result.add(this.stack.get(i));
			if(!copy && this.stack.get(i).match(f))
				copy = true;
		}
		
		
		return new CallStack(result);
	}
	
	/**
	 * @return the last frame
	 */
	public Frame getLastFrame() {
		return this.stack.get(this.stack.size()-1);
	}
	
	/**
	 * @return the first frame
	 */
	public Frame getFirstFrame() {
		return this.stack.get(0);
	}
}
