package dimmunix.hybrid;

import java.util.Iterator;

import dimmunix.Pair;
import dimmunix.Vector;


public class ThreadNode {	
	private enum ReqMode {READ, WRITE};//for rw locks
	
	private ResourceNode reqResource = null;
	private StackTraceElement reqPos = null;	
	private ReqMode reqMode = null;//for rw locks
	
	public final long id;
	
	private Vector<StackTraceElement> positionsInInstances = new Vector<StackTraceElement>();
	
	public ThreadNode(long id) {
		this.id = id;
	}
	
	public void mutexReq(ResourceNode r, StackTraceElement pos) {
		this.reqResource = r;
		this.reqPos = pos;
	}
	
	public void readLockReq(ResourceNode r, StackTraceElement pos) {
		this.reqMode = ReqMode.READ;
		this.reqResource = r;
		this.reqPos = pos;
	}
	
	public void writeLockReq(ResourceNode r, StackTraceElement pos) {
		this.reqMode = ReqMode.WRITE;
		this.reqResource = r;
		this.reqPos = pos;
	}
	
	public void semaphoreReq(ResourceNode r, StackTraceElement pos) {
		this.reqResource = r;
		this.reqPos = pos;
	}
	
	public void removeRequest() {
		this.reqMode = null;
		this.reqResource = null;
		this.reqPos = null;
	}
	
	public StackTraceElement getReqPos() {
		return this.reqPos;
	}
	
	public ResourceNode getReqResource() {
		return this.reqResource;
	}
	
	private boolean getDeadlock(ThreadNode tDst, Vector<StackTraceElement> sig, Vector<ThreadNode> threads) {
		if (this.reqResource == null || !this.reqResource.isAcquired()) {
			return false;
		}
		if (this.reqResource.getType().equals("MUTEX")) {
			Vector<StackTraceElement> sigTail = new Vector<StackTraceElement>();
			Vector<ThreadNode> threadsTail = new Vector<ThreadNode>();
			ThreadNode to = this.reqResource.getOwner();
			boolean result = to == tDst || to.getDeadlock(tDst, sigTail, threadsTail);
			if (result) {
				sig.add(this.reqPos);
				sig.add(this.reqResource.getAcqPosition());
				sig.addAll(sigTail);
				threads.add(this);
				threads.addAll(threadsTail);
			}
			return result;
		}
		if (this.reqResource.getType().equals("RWLOCK")) {
			if (this.reqMode == ReqMode.READ && this.reqResource.isAcquiredRead()) {
				return false;
			}
			for (ThreadNode to: this.reqResource.getOwners()) {
				Vector<StackTraceElement> sigTail = new Vector<StackTraceElement>();
				Vector<ThreadNode> threadsTail = new Vector<ThreadNode>();
				boolean result = to == tDst || to.getDeadlock(tDst, sigTail, threadsTail);
				if (result) {
					sig.add(this.reqPos);
					sig.add(this.reqResource.getAcqPosition(to));
					sig.addAll(sigTail);
					threads.add(this);
					threads.addAll(threadsTail);
					return true;
				}
			}
			return false;
		}
		if (this.reqResource.getType().equals("SEMAPHORE")) {
			Vector<StackTraceElement> sigResult = new Vector<StackTraceElement>(); 
			Vector<ThreadNode> threadsResult = new Vector<ThreadNode>();
			for (ThreadNode to: this.reqResource.getOwners()) {
				Vector<StackTraceElement> sigTail = new Vector<StackTraceElement>();
				Vector<ThreadNode> threadsTail = new Vector<ThreadNode>();
				boolean result = to == tDst || to.getDeadlock(tDst, sigTail, threadsTail);
				if (!result) {
					return false;
				}
				if (sigResult.isEmpty()) {
					sigResult.add(this.reqPos);
					sigResult.add(this.reqResource.getAcqPosition(to));
					sigResult.addAll(sigTail);
					threadsResult.add(this);
					threadsResult.addAll(threadsTail);
				}
			}
			sig.addAll(sigResult);
			threads.addAll(threadsResult);
			return true;
		}
		//should not get here
		return false;
	}
	
	public Pair<Signature, Vector<ThreadNode>> getDeadlock() {
		Vector<StackTraceElement> sig = new Vector<StackTraceElement>();
		Vector<ThreadNode> dlckThreads = new Vector<ThreadNode>();
		boolean result = this.getDeadlock(this, sig, dlckThreads);
		if (result) {
			Vector<StackTraceElement> innerPositions = new Vector<StackTraceElement>();
			Vector<StackTraceElement> outerPositions = new Vector<StackTraceElement>();
			Iterator<StackTraceElement> sigIt = sig.iterator();
			while (sigIt.hasNext()) {
				innerPositions.add(sigIt.next());
				outerPositions.add(sigIt.next());
			}
			return new Pair<Signature, Vector<ThreadNode>>(new Signature(outerPositions, innerPositions), dlckThreads) ;
		}
		return null;
	}
	
	public int hashCode() {
		return (int)this.id;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ThreadNode) {
			ThreadNode t = (ThreadNode)obj;
			return this.id == t.id;
		}
		return false;
	}
	
	public void addInstancePosition(StackTraceElement p) {
		this.positionsInInstances.add(p);
	}
	
	public boolean removeInstancePosition(StackTraceElement p) {
		return this.positionsInInstances.remove(p);
	}
	
	public boolean isPositionInInstances(StackTraceElement p) {
		return this.positionsInInstances.contains(p);
	}
}
