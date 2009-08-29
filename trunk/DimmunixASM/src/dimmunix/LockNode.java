package dimmunix;

public class LockNode extends Node {
	int id;
	int nlck = 0;
	
	ThreadNode owner = null;
	Position acqPos = null;
	boolean acqPosInHistory = false;
	
	long grantTime = 0;
	
	public LockNode(int objId) {
		this.id = objId;
	}

	public String toString() {
		return ""+ this.id;
	}
	
	public int hashCode() {
		return this.id;
	}

	public boolean equals(Object n) {
		if (n == null || !(n instanceof LockNode))
			return false;
		return this.id == ((LockNode)n).id;
	}
}
