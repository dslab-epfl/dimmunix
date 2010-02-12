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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockGrant {
	ThreadNode thread;
	LockNode lock;
	Position position;
	volatile int n = 0;
	ConcurrentLinkedQueue<ThreadNode> yielders = new ConcurrentLinkedQueue<ThreadNode>();
	ReentrantReadWriteLock yieldersLock = new ReentrantReadWriteLock();
	volatile long time = 0; 
	
	public LockGrant(ThreadNode thread, LockNode lock, Position position) {
		this.thread = thread;
		this.lock = lock;
		this.position = position;
	}

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof LockGrant))
			return false;
		LockGrant lg = (LockGrant)obj;
		return thread == lg.thread && lock == lg.lock;
	}
	
	boolean disjoint(LockGrant lg) {
		return thread != lg.thread && (lg.lock == null || lock != lg.lock);
	}
}
