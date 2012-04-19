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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dimmunix.CallStack;
import dimmunix.Util;
import dimmunix.Vector;

public class ResourceAllocationGraph {

	DimmunixDeadlock dimmunix;
	
	ConcurrentHashMap<Integer, LockNode> locks = new ConcurrentHashMap<Integer, LockNode>(65536);
	ReentrantReadWriteLock locksRWLock = new ReentrantReadWriteLock();
	Vector<ThreadNode> threads = new Vector<ThreadNode>(20000);
	ReentrantReadWriteLock threadsRWLock = new ReentrantReadWriteLock();
	ConcurrentHashMap<CallStack, Position> positions = new ConcurrentHashMap<CallStack, Position>(65536);			
	ReentrantReadWriteLock positionsRWLock = new ReentrantReadWriteLock();

	HashSet<ThreadNode> yieldingThreads = new HashSet<ThreadNode>();
	
//	Vector<LockNode> preallocatedLocks = new Vector<LockNode>(512);
//	Vector<Position> preallocatedPositions = new Vector<Position>(4096);
	
	HashSet<Node> edges = new HashSet<Node>();
	
	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	public ResourceAllocationGraph(DimmunixDeadlock dimmunix) {
		this.dimmunix = dimmunix;
		
		for (int i = 0; i < threads.capacity(); i++)
			threads.add(new ThreadNode(null));

//		for (int i = 0; i < preallocatedLocks.capacity(); i++)
//			preallocatedLocks.add(new LockNode(0));
		
//		for (int i = 0; i < preallocatedPositions.capacity(); i++)
//			preallocatedPositions.add(new Position(null));
	}
	
	LockNode getNewLockNode(int objId) {		
/*		if (preallocatedLocks.isEmpty()) {
			for (int i = 0; i < preallocatedLocks.capacity(); i++)
				preallocatedLocks.add(new LockNode(0));
		}
		
		LockNode l = preallocatedLocks.remove();
		l.id = objId;
		return l;
		*/
		
		return new LockNode(objId);
	}
	
	Position getNewPosition(CallStack callStack) {
		return new Position(callStack); 
/*		synchronized (positions) {
			if (preallocatedPositions.isEmpty()) {
				for (int i = 0; i < preallocatedPositions.capacity(); i++)
					preallocatedPositions.add(new Position(null));
			}
			Position p = preallocatedPositions.remove();		
			p.callStack = callStack;
			dimmunix.refreshMatchingPositions(p);
			return p;			
		}*/		
	}
	
	ThreadNode getThreadNode(int tid) {
/*		try {
			this.threadsRWLock.readLock().lock();
			
			if (tid >= this.threads.size()) {
				try {
					this.threadsRWLock.readLock().unlock();
					this.threadsRWLock.writeLock().lock();
					
					if (tid >= this.threads.size()) {
						int n = threads.size();
						this.threads.setSize(tid+ 100);
						for (int i = n; i < threads.size(); i++)
							threads.set(i, new ThreadNode(null));						
					}					
				}
				finally {
					this.threadsRWLock.readLock().lock();
					this.threadsRWLock.writeLock().unlock();
				}
			}
			return this.threads.get(tid); 			
		}
		finally {
			this.threadsRWLock.readLock().unlock();
		}*/
		
		return this.threads.get(tid);
	}
	
	ThreadNode getThreadNode(Thread thread) {
		int tid = (int)thread.getId();
		ThreadNode tnode = this.getThreadNode(tid);
		if (tnode.thread != thread) {
			tnode.thread = thread;
			tnode.id = tid;
		}
		return tnode;
	}
	
	LockNode getLockNode(Object obj) {
		int id = System.identityHashCode(obj);
		return this.getLockNode(id);
	}
	
	LockNode findLockNode(int id, Vector<LockNode> group) {
		for (int i = 0; i < group.size(); i++) {
			if (group.get(i).id == id)
				return group.get(i);
		}
		return null;
	}
	
	LockNode getLockNode(int objId) {
/*		try {
			this.locksRWLock.readLock().lock();

			Vector<LockNode> group = this.locks.get(objId);
			LockNode l;
			if (group == null) {
				try {
					this.locksRWLock.readLock().unlock();
					this.locksRWLock.writeLock().lock();
					
					group = this.locks.get(objId); 
					if (group == null) {
						group = new Vector<LockNode>();
						l = this.getNewLockNode(objId);
						group.add(l);
						this.locks.put(objId, group);
						return l;					
					}
				}
				finally {
					this.locksRWLock.readLock().lock();
					this.locksRWLock.writeLock().unlock();
				}
			}
			l = this.findLockNode(objId, group);
			if (l == null) {
				try {
					locksRWLock.readLock().unlock();
					locksRWLock.writeLock().lock();
					
					l = this.findLockNode(objId, group);
					if (l == null) {
						l = this.getNewLockNode(objId);					
						group.add(l);					
						return l;
					}
				}
				finally {
					locksRWLock.readLock().lock();
					locksRWLock.writeLock().unlock();
				}
			}
			return l;
		} 
		finally {
			this.locksRWLock.readLock().unlock();
		}*/
		
		Integer id = objId;
		LockNode l = this.locks.get(id);
		if (l == null) {
			LockNode lNew = this.getNewLockNode(objId);
			LockNode lOld = this.locks.putIfAbsent(id, lNew);
			if (lOld != null)
				return lOld;
			else
				return lNew;
		}
		else {
			return l;
		}
	}
	
	Position findPosition(CallStack callStack, Vector<Position> group) {
		for (int i = 0; i < group.size(); i++) {
			if (group.get(i).callStack.equals(callStack))
				return group.get(i);
		}		
		return null;
	}
	
	Position getPosition(CallStack callStack) {
		if (callStack == null)
			return null;
/*		try {
			positionsRWLock.readLock().lock();
			
			Vector<Position> group = this.positions.get(callStack);
			Position p;
			if (group == null) {
				try {
					positionsRWLock.readLock().unlock();
					positionsRWLock.writeLock().lock();
					
					group = positions.get(callStack);
					if (group == null) {
						group = new Vector<Position>();
						CallStack newCallStack = callStack.cloneStack();
						p = getNewPosition(newCallStack);
						group.add(p);
						positions.put(newCallStack, group);
						return p;										
					}
				}
				finally {
					positionsRWLock.readLock().lock();
					positionsRWLock.writeLock().unlock();
				}
			}
			p = this.findPosition(callStack, group);
			if (p == null) {
				try {
					positionsRWLock.readLock().unlock();
					positionsRWLock.writeLock().lock();
					
					p = this.findPosition(callStack, group);
					if (p == null) {
						p = getNewPosition(callStack.cloneStack());					
						group.add(p);					
						return p;					
					}
				}
				finally {
					positionsRWLock.readLock().lock();
					positionsRWLock.writeLock().unlock();
				}
			}
			return p;									
		}
		finally {
			positionsRWLock.readLock().unlock();
		}*/
		
		Position p = this.positions.get(callStack);
		if (p == null) {
			CallStack newCallStack = callStack.cloneStack();
			Position pNew = getNewPosition(newCallStack);
			Position pOld = this.positions.putIfAbsent(newCallStack, pNew);
			if (pOld != null) {
				return pOld;
			}
			else {
//				if (dimmunix.enableDynamicMatchDepth) {
//					dimmunix.refreshMatchingPositions(pNew);
//				}
				return pNew;
			}
		}
		else {
			return p;
		}
	}
	
	void request(ThreadNode t, LockNode l, Position p) {
		t.next = l;
		t.posNext = p;
//		t.hasHiddenLockRequest = false;
	}
	
	void lock(ThreadNode t, LockNode l, Position p) {
		l.next = t;
		l.posNext = p;
		t.next = null;
		t.posNext = null;		
	}
	
	void unlock(ThreadNode t, LockNode l) {
		l.next = null;
		l.posNext = null;
	}	
	
	void update() {
		for (Node x: this.edges) {
			x.next = null;
			x.posNext = null;
		}
		this.edges.clear();
		
		for (ThreadNode yt: this.yieldingThreads) {
			if (yt.threadYields.isEmpty()) {
				this.updateFrom(yt);				
			}
			else {
				for (int i = 0; i < yt.threadYields.size(); i++) {
//					if (yt.lockYields.get(i).owner != yt.threadYields.get(i))
//						break;
					this.updateFrom(yt.threadYields.get(i));				
				}							
			}
		}
	}
	
	void updateFrom(ThreadNode t) {
		if (this.edges.contains(t))
			return;
		
		ThreadInfo tinfo = threadMXBean.getThreadInfo(t.thread.getId());
		if (tinfo == null)
			return;
		
		t.next = t.semNext;
		
		if (t.next != null) {
			this.edges.add(t);
			
			Node l = t.next;
			
			if (this.edges.contains(l))
				return;
			
			l.next = l.semNext;
			if (l.next == null)
				return;				
			ThreadNode tOwner = (ThreadNode)l.next;
			this.edges.add(l);
			
			this.updateFrom(tOwner);
			
			return;
		}

		LockInfo lWait = tinfo.getLockInfo();
		if (lWait != null) {
			LockNode l = this.getLockNode(lWait.getIdentityHashCode());
			t.next = l;
			this.edges.add(t);
			
			if (this.edges.contains(l))
				return;
			
			long ownerId = tinfo.getLockOwnerId();
			if (ownerId == -1)
				return;			
			ThreadNode tOwner = this.getThreadNode(Util.getThread(ownerId));
			l.next = tOwner;
			this.edges.add(l);
			
			this.updateFrom(tOwner);
		}
	}	
}