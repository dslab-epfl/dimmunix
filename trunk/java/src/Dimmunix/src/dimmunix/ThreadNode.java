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

import java.util.Iterator;

public class ThreadNode extends Node {
	Thread thread;
	Position reqPos = null;
	Object yieldLock = new Object();
	
	Dimmunix dImmunix;
	
	Vector<ThreadNode> threadYields = new Vector<ThreadNode>(10);
	Vector<Position> posYields = new Vector<Position>(10);
	Signature yieldCauseTemplate = null;
	Instance templateInstance = new Instance();
	Instance yieldCause = new Instance();
	
	CallStack currentCallStack = new CallStack(10);	
	Vector<Iterator<LockGrant>> currentLockGrantsIterators = new Vector<Iterator<LockGrant>>(10);
	Vector<Vector<Position>> currentMatchingPositions = new Vector<Vector<Position>>(10);
	Vector<Position> currentPositionsToMatch = new Vector<Position>(10);
	Vector<Signature> currentTemplates = new Vector<Signature>(10);
	Vector<Integer> currentIndicesInTemplates = new Vector<Integer>(10);
	
	boolean reqPosInHistory = false;
	LockGrant currentLockGrant;
		
	Vector<LockGrant> preallocatedLockGrants = new Vector<LockGrant>(100);
	Vector<LockGrant> removedLockGrants = new Vector<LockGrant>(100);
	
	Vector<Event> lockOps = new Vector<Event>(100);
	
	int nSyncs = 0;
	
	boolean bypassAvoidance = false;
	
	public String toString() {
		return this.thread.getName();
	}
	
	public ThreadNode(Thread thread, Dimmunix dImmunix) {
		this.thread = thread;
		this.dImmunix = dImmunix;
		
		for (int i = 0; i < currentMatchingPositions.capacity(); i++) {
			currentMatchingPositions.add(new Vector<Position>(10));
		}
	}

	public int hashCode() {
		return (int)this.thread.getId();
	}

	public boolean equals(Object n) {
		if (n == null || !(n instanceof ThreadNode))
			return false;
		return this.thread == ((ThreadNode)n).thread;
	}
	
	boolean allYieldsGrey() {
		for (int i = 0; i < threadYields.size(); i++) {
			if (threadYields.get(i).color != Color.GREY)
				return false;			
		}
		return true;
	}		
	
	void resetYieldCauseTo(Instance instance) {
		this.yieldCause.setSize(instance.size());
		for (int i = 0; i < instance.size(); i++)
			this.yieldCause.lockGrants.set(i, instance.lockGrants.get(i));
		this.yieldCause.template = instance.template;
	}
	
	void resumeFromLivelock() {
		synchronized (yieldLock) {
			yieldLock.notify();
			yieldCause.clear();
			this.bypassAvoidance = true;
		}
		threadYields.clear();
		posYields.clear();
		yieldCauseTemplate = null;
	}
	
	boolean isNotified() {
		return this.yieldCause.isEmpty();
	}
}
