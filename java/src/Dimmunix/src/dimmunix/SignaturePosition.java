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

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SignaturePosition {
	Position value;
	volatile int depth;
	Dimmunix dimmunix;
	
	Vector<Position> matchingPositions = new Vector<Position>();
	ReentrantReadWriteLock rwLockMatching = new ReentrantReadWriteLock();
	
	int nFPs = 0;
	int nTPs = 0;
	
	public SignaturePosition(Position value, int depth, Dimmunix dImmunix) {
		this.value = value;
		this.depth = depth;
		this.dimmunix = dImmunix;
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SignaturePosition))
			return false;
		SignaturePosition tpos = ((SignaturePosition)obj);
		return value == tpos.value;
	}

	public String toString() {
		return depth+ "#"+ value;
	}		
	
	boolean match(Position pos) {
		if (depth > pos.size())
			return false;
		for (int i = 0; i < depth; i++) {
			if (!value.callStack.get(i).equals(pos.callStack.get(i)))
				return false;
		}
		return true;
	}
	
	void incrementPrecision() {
		depth++;
		refreshExistingMatchingPositions();
	}
	
	void decrementPrecision() {
		depth--;
		refreshMatchingPositionsInHist();
	}
	
	void refreshMatchingPositionsInHist() {
		rwLockMatching.writeLock().lock();
		
		matchingPositions.clear();		
		matchingPositions.add(this.value);
		for (Signature tmpl: dimmunix.history) {
			for (int i = 0; i < tmpl.size(); i++) {
				Position pos = tmpl.positions.get(i).value;
				if (!matchingPositions.containsRef(pos) && this.match(pos))
					matchingPositions.add(pos);
			}
		}
		
		rwLockMatching.writeLock().unlock();
	}
	
	void refreshExistingMatchingPositions() {
		rwLockMatching.writeLock().lock();
		
		for (int i = 0; i < matchingPositions.size(); i++) {
			if (!this.match(matchingPositions.get(i)))
				matchingPositions.remove(i--);
		}
		
		rwLockMatching.writeLock().unlock();
	}	
	
	void getMatchingPositions(Vector<Position> positions) {
		rwLockMatching.readLock().lock();
		matchingPositions.copyInto(positions);
		rwLockMatching.readLock().unlock();
	}	
	
	void matchAndAdd(Position p) {
		rwLockMatching.writeLock().lock();
		if (match(p))
			matchingPositions.add(p);
		rwLockMatching.writeLock().unlock();		
	}
}
