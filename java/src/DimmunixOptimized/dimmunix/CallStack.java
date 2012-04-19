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

import java.util.Collections;
import java.util.List;


public class CallStack {
	private Vector<StackTraceElement> frames;
	public int size;
	
	public CallStack(int depth) {
		this.frames = new Vector<StackTraceElement>(depth);
		this.size = 0;
	}
	
	public CallStack(Vector<StackTraceElement> frames) {
		this.frames = frames;
		this.size = frames.size();
	}

	public CallStack(StackTraceElement[] frames, int depth) {
		this.frames = new Vector<StackTraceElement>();
		for (StackTraceElement f: frames) {
			if (this.frames.size() > depth)
				break;
			this.frames.add(f);
		}
		this.size = this.frames.size();
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
//		if (frame.getLineNumber() >= 0)
		this.frames.add(frame);
		size++;
	}

	public void add(String frame) {
		StackTraceElement f = Util.parsePosition(frame); 
		this.frames.add(f);
		size++;
	}

	public void clear() {
		this.frames.clear();
		size = 0;
	}
	
	public boolean match(CallStack cs, int depth) {
		if (depth > cs.size)
			return false;
		for (int i = 0; i < depth; i++) {
			if (!this.frames.get(i).equals(cs.frames.get(i)))
				return false;
		}
		
		return true;
	}	
	
	public List<StackTraceElement> getFrames() {
		return Collections.unmodifiableList(this.frames);
	}
}
