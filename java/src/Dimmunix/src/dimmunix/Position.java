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

import java.util.concurrent.ConcurrentLinkedQueue;

public class Position {
	CallStack callStack;		
	ConcurrentLinkedQueue<LockGrant> lockGrants;	
	
	public Position(CallStack callStack, boolean inHistory) {
		this.callStack = callStack;
		lockGrants = new ConcurrentLinkedQueue<LockGrant>();
	}
	
	LockGrant getNewLockGrant(ThreadNode t, LockNode l) {
		for (int i = 0; i < t.removedLockGrants.size(); i++) {
			LockGrant lg = t.removedLockGrants.get(i);
			if (lg.thread == t && lg.lock == l && lg.position == this) {
				lg.n = 1;
				t.removedLockGrants.removeFast(i);
				return lg;
			}
		}
		if (t.preallocatedLockGrants.isEmpty()) {
			for (int i = 0; i < t.preallocatedLockGrants.capacity(); i++)
				t.preallocatedLockGrants.add(new LockGrant(null, null, null));
		}
		LockGrant lg = t.preallocatedLockGrants.remove();
		lg.thread = t;
		lg.lock = l;
		lg.position = this;
		lg.n = 1;
		lg.time = System.nanoTime();
		return lg;
	}
	
	LockGrant grant(ThreadNode t, LockNode l) {		
		LockGrant lg = findLockGrant(t, l); 
		if (lg != null) {
			lg.n++;
			return lg;
		}
		lg = getNewLockGrant(t, l);
		lockGrants.add(lg);
		return lg;
	}
	
	LockGrant findLockGrant(ThreadNode t, LockNode l) {
		for (LockGrant lg: lockGrants) {
			if (lg.thread == t && lg.lock == l)
				return lg;
		}
		return null;
	}
	
	int ungrant(LockGrant lg) {
		lg.n--;
		if (lg.n > 0)
			return lg.n;
		lockGrants.remove(lg);
		/*if (lg.thread.removedLockGrants.size() == lg.thread.removedLockGrants.capacity())
			lg.thread.removedLockGrants.clear();*/
		lg.thread.removedLockGrants.add(lg);
		return lg.n;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Position)) {
			return false;
		}
		return this.callStack.equals(((Position)obj).callStack);
	}

	@Override
	public int hashCode() {
		return this.callStack.hashCode();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < callStack.size(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(callStack.get(i));			
		}
		return sb.toString();
	}	
	
	public int size() {
		return callStack.size();
	}	
}
