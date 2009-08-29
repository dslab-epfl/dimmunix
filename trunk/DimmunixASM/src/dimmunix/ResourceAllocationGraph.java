package dimmunix;

import java.util.HashMap;

public class ResourceAllocationGraph {

	Dimmunix dimmunix;
	
	HashMap<Integer, Vector<LockNode>> locks = new HashMap<Integer, Vector<LockNode>>(512);
	Vector<ThreadNode> threads = new Vector<ThreadNode>(1100);
	HashMap<CallStack, Vector<Position>> positions = new HashMap<CallStack, Vector<Position>>(4096);			
	
	Vector<ThreadNode> requestingThreads = new Vector<ThreadNode>(500);
	
	Vector<LockNode> preallocatedLocks = new Vector<LockNode>(512);
	Vector<Position> preallocatedPositions = new Vector<Position>(4096);
	
	public ResourceAllocationGraph(Dimmunix dimmunix) {
		this.dimmunix = dimmunix;
		
		for (int i = 0; i < threads.capacity(); i++)
			threads.add(new ThreadNode(null, dimmunix));

		for (int i = 0; i < preallocatedLocks.capacity(); i++)
			preallocatedLocks.add(new LockNode(0));
		
		for (int i = 0; i < preallocatedPositions.capacity(); i++)
			preallocatedPositions.add(new Position(null, false));
	}
	
	LockNode getNewLockNode(int objId) {
		Logger.nLocks++;
		
		if (preallocatedLocks.isEmpty()) {
			for (int i = 0; i < preallocatedLocks.capacity(); i++)
				preallocatedLocks.add(new LockNode(0));
		}
		
		LockNode l = preallocatedLocks.remove();
		l.id = objId;
		return l;
	}
	
	Position getNewPosition(CallStack callStack) {
		Logger.nPositions++;
		
		if (preallocatedPositions.isEmpty()) {
			for (int i = 0; i < preallocatedPositions.capacity(); i++)
				preallocatedPositions.add(new Position(null, false));
		}
		Position p = preallocatedPositions.remove();		
		p.callStack = callStack;
		dimmunix.refreshMatchingPositions(p);
		return p;
	}
	
	ThreadNode getThreadNode(Thread thread) {
		int tid = (int)thread.getId();
		if (tid >= this.threads.size()) {
			synchronized (this.threads) {
				if (tid >= this.threads.size()) {
					int n = threads.size();
					this.threads.setSize(tid+ 100);
					for (int i = n; i < threads.size(); i++)
						threads.set(i, new ThreadNode(null, dimmunix));						
				}
			}
		}
		ThreadNode tnode = this.threads.get(tid); 
		if (tnode.thread != thread)//might be a new thread reusing tid, or tnode.thread == null
			tnode.thread = thread;
		return tnode;
	}
	
	LockNode getLockNode(Object obj) {
		return this.getLockNode(System.identityHashCode(obj));
	}
	
	LockNode findLockNode(int id, Vector<LockNode> group) {
		for (int i = 0; i < group.size(); i++) {
			if (group.get(i).id == id)
				return group.get(i);
		}
		return null;
	}
	
	LockNode getLockNode(int objId) {
		Vector<LockNode> group = this.locks.get(objId);
		LockNode l;
		if (group == null) {
			synchronized (this.locks) {
				group = this.locks.get(objId); 
				if (group == null) {
					group = new Vector<LockNode>();
					l = this.getNewLockNode(objId);
					group.add(l);
					this.locks.put(objId, group);
					return l;					
				}
			}
		}
		l = this.findLockNode(objId, group);
		if (l == null) {
			synchronized (group) {
				l = this.findLockNode(objId, group);
				if (l == null) {
					l = this.getNewLockNode(objId);					
					group.add(l);					
					return l;
				}
			}
		}
		return l;
	}
	
	Position findPosition(CallStack callStack, Vector<Position> group) {
		for (int i = 0; i < group.size(); i++) {
			if (group.get(i).callStack.equals(callStack))
				return group.get(i);
		}		
		return null;
	}
	
	Position getPosition(CallStack callStack) {
		Vector<Position> group = this.positions.get(callStack);
		Position p;
		if (group == null) {
			synchronized (positions) {
				group = positions.get(callStack);
				if (group == null) {
					group = new Vector<Position>();
					CallStack newCallStack = callStack.cloneStack();
					p = getNewPosition(newCallStack);
					group.add(p);
					positions.put(callStack, group);
					return p;										
				}
			}
		}
		p = this.findPosition(callStack, group);
		if (p == null) {
			synchronized (positions) {
				p = this.findPosition(callStack, group);
				if (p == null) {
					p = getNewPosition(callStack.cloneStack());					
					group.add(p);					
					return p;					
				}
			}
		}
		return p;									
	}
	
	void request(ThreadNode t, LockNode l, Position p) {
		t.next = l;
		t.posNext = p;
		requestingThreads.add(t);
	}
	
	void lock(ThreadNode t, LockNode l, Position p) {
		l.next = t;
		l.posNext = p;
		t.next = null;
		t.posNext = null;		
		requestingThreads.remove(t);
	}
	
	void unlock(ThreadNode t, LockNode l) {
		l.next = null;
		l.posNext = null;
	}	
}
