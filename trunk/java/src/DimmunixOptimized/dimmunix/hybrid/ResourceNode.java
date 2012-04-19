package dimmunix.hybrid;

import java.util.Collections;
import java.util.List;

import dimmunix.Vector;

public class ResourceNode {
	private enum ResType {MUTEX, RWLOCK, SEMAPHORE};
	private enum AcqMode {READ, WRITE};
	
	private final ResType type;
	private final Object resource;
	
	private Vector<ThreadNode> owners = new Vector<ThreadNode>();//for all resource types
	private Vector<StackTraceElement> positions = new Vector<StackTraceElement>();//for all resource types
	private int permits = 0;//for semaphores
	private Vector<AcqMode> acqModes = new Vector<ResourceNode.AcqMode>();//for rw locks
	
	private ResourceNode(ResType type, Object obj) {
		this.type = type;
		this.resource = obj;
	}
	
	public static ResourceNode newMutex(Object obj) {
		return new ResourceNode(ResType.MUTEX, obj);
	}
	
	public static ResourceNode newRWLock(Object obj) {
		return new ResourceNode(ResType.RWLOCK, obj);
	}
	
	public static ResourceNode newSemaphore(Object obj, int permits) {
		ResourceNode r = new ResourceNode(ResType.SEMAPHORE, obj);
		r.permits = permits;
		return r;
	}
	
	public void lock(ThreadNode t) {
		this.owners.add(t);
		this.positions.add(t.getReqPos());
		t.removeRequest();
	}
	
	public void unlock() {
		this.owners.remove();
		this.positions.remove();
	}
	
	public void lockr(ThreadNode t) {
		this.owners.add(t);
		this.positions.add(t.getReqPos());
		this.acqModes.add(AcqMode.READ);
		t.removeRequest();
	}
	
	public void unlockr(ThreadNode t) {
		int idxT = this.owners.lastIndexOf(t);
		this.owners.remove(idxT);
		this.positions.remove(idxT);
		this.acqModes.remove(idxT);		
	}
	
	public void lockw(ThreadNode t) {
		this.owners.add(t);
		this.positions.add(t.getReqPos());
		this.acqModes.add(AcqMode.WRITE);
		t.removeRequest();
	}
	
	public void unlockw(ThreadNode t) {
		int idxT = this.owners.lastIndexOf(t);
		this.owners.remove(idxT);
		this.positions.remove(idxT);
		this.acqModes.remove(idxT);		
	}
	
	public void acquire(ThreadNode t) {
		this.owners.add(t);
		this.positions.add(t.getReqPos());		
		this.permits--;
		t.removeRequest();
	}	
	
	public void release(ThreadNode t) {
		int idxT = this.owners.lastIndexOf(t);
		if (idxT != -1) {
			this.owners.remove(idxT);
			this.positions.remove(idxT);
		}
		this.permits++;
	}
	
	public ThreadNode getOwner() {
		if (this.owners.isEmpty()) {
			return null;
		}		
		return this.owners.get(0);
	}
	
	public StackTraceElement getAcqPosition() {
		if (this.owners.isEmpty()) {
			return null;
		}		
		return this.positions.get(0);
	}
	
	public List<ThreadNode> getOwners() {
		return Collections.unmodifiableList(this.owners);
	}
	
	public StackTraceElement getAcqPosition(ThreadNode t) {
		for (int i = 0; i < this.owners.size(); i++) {
			if (this.owners.get(i) == t) {
				return this.positions.get(i);
			}
		}
		
		return null;
	}
	
	public int getPermits() {
		return this.permits;
	}
	
	public boolean isAcquired() {
		return !this.owners.isEmpty();
	}
	
	public boolean isAcquiredRead() {
		if (this.acqModes.isEmpty()) {
			return false;
		}
		for (AcqMode m: this.acqModes) {
			if (m == AcqMode.WRITE) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isAcquiredWrite() {
		if (this.acqModes.isEmpty()) {
			return false;
		}
		for (AcqMode m: this.acqModes) {
			if (m == AcqMode.WRITE) {
				return true;
			}
		}
		return false;
	}
	
	public String getType() {
		return this.type.toString(); 
	}
	
	public int hashcode() {
		return System.identityHashCode(resource);
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ResourceNode) {
			ResourceNode res = (ResourceNode)obj;
			return this.resource == res.resource;
		}
		return false;
	}
}
