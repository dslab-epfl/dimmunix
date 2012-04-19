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

import java.util.HashSet;
import java.util.Iterator;

import dimmunix.CallStack;
import dimmunix.Vector;

public class ThreadNode extends Node {
	Thread thread;
	int id;
	CallStack reqPos = null;
	Object yieldLock = new Object();

	Vector<ThreadNode> threadYields = new Vector<ThreadNode>();//for event processor
	Vector<LockNode> lockYields = new Vector<LockNode>();//for event processor
	Vector<Position> posYields = new Vector<Position>();//for event processor
	Signature yieldCauseTemplate = null;//for event processor
	
	Instance curInstance = new Instance();

	CallStack currentCallStack = new CallStack(100);
	Vector<StackTraceElement> currentStackTrace = new Vector<StackTraceElement>(100);
	Vector<Iterator<LockGrant>> currentLockGrantsIterators = new Vector<Iterator<LockGrant>>();
	Vector<Signature> currentTemplates = new Vector<Signature>();
	Vector<SignaturePosition> currentMatchingPositions = new Vector<SignaturePosition>();

	boolean reqPosInHistory = false;
	Vector<LockGrant> currentLockGrants = new Vector<LockGrant>();

	Vector<LockGrant> removedLockGrants = new Vector<LockGrant>(100);

	Vector<Instance> instancesToSkip = new Vector<Instance>(10);
	
	// <-communix
	HashSet<LockNode> locksHeld = new HashSet<LockNode>();
	// ->
	
	public String toString() {
		return this.thread.getName();
	}

	public ThreadNode(Thread thread) {
		this.thread = thread;
	}

	public int hashCode() {
		return (int) this.thread.getId();
	}

	public boolean equals(Object n) {
		if (n == null || !(n instanceof ThreadNode))
			return false;
		return this.thread == ((ThreadNode) n).thread;
	}

	boolean allYieldsGrey() {
		for (int i = 0; i < threadYields.size(); i++) {
			if (threadYields.get(i).color != Color.GREY)
				return false;
		}
		return true;
	}

	void bypassLivelock() {
		this.instancesToSkip.add(this.curInstance.cloneInstance());
		
		int deadlockSize = this.curInstance.template.size();
		LockGrant otherLockGrant = null;
		for (LockGrant lg: this.curInstance.lockGrants) {
			if (lg.thread != this) {
				otherLockGrant = lg;
			}
		}
//		Object yieldLock = (deadlockSize > 2)? this.yieldLock: otherLockGrant;
		Object yieldLock = this.yieldLock;
		synchronized (yieldLock) {
			yieldLock.notifyAll();
		}		
	}

/*	void sortMatchingTemplates() {
		for (int i = 0; i < this.currentTemplates.size()- 1; i++) {
			for (int j = i+ 1; j < this.currentTemplates.size(); j++) {
				if (this.currentTemplates.get(i).id > this.currentTemplates.get(j).id) {
					Signature sig = this.currentTemplates.get(i);
					this.currentTemplates.set(i, this.currentTemplates.get(j));
					this.currentTemplates.set(j, sig);					
				}
			}
		}
	}*/
	
	LockGrant findLockGrant(LockNode l, Position p) {
		for (int i = 0; i < this.currentLockGrants.size(); i++) {
			LockGrant lg = this.currentLockGrants.get(i);
			if (lg.lock == l && lg.position == p)
				return lg;
		}
		return null;
	}
}