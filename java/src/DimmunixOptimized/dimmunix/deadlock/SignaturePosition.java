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

package dimmunix.deadlock;

import dimmunix.CallStack;


public class SignaturePosition {
	public Position value;
	volatile int depth;
	
	Signature signature;
	int index;
	
	int[] matches = new int[2500];

	int nFPs = 0;
	int nTPs = 0;
	
	public SignaturePosition(Position value, int depth, Signature sig, int index) {
		this.value = value;
		this.depth = depth;
		
		this.signature = sig;
		this.index = index;
		
		for (int i = 0; i < matches.length; i++) {
			matches[i] = Integer.MAX_VALUE;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SignaturePosition))
			return false;
		SignaturePosition pos = ((SignaturePosition) obj);
		return value == pos.value && index == pos.index && signature == pos.signature;
	}

	@Override
	public String toString() {
		return depth + "#" + value;
	}

	boolean match(CallStack pos) {
		return value.callStack.match(pos, depth);
	}

	void incrementPrecision() {
		if (depth < this.value.size()) {
			depth++;
		}
	}

	void decrementPrecision() {
		if (depth > 1) {
			depth--;
		}
	}
	
	void shrink(int newSize) {
		if (newSize >= this.value.size()) {
			return;
		}
		
		CallStack cs = new CallStack(newSize);
		for (int i = 0; i < newSize; i++) {
			cs.add(this.value.callStack.get(i));
		}
		
		this.value = DimmunixDeadlock.instance.rag.getPosition(cs);
		this.depth = Math.min(this.depth, this.value.size());
	}
}
