/*
     Created by Horatiu Jula, George Candea, Daniel Tralamazza, Cristian Zamfir
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix.

     Dimmunix is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

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
		if (frame.getLineNumber() >= 0)
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
		else
			return;
		
		this.frames.add(new StackTraceElement(declaringClass, methodName, fileName, lineNumber));
	}

	public void clear() {
		this.frames.clear();
	}
}
