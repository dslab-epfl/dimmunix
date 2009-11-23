package dimmunix;


public class Instance {
	Vector<LockGrant> lockGrants = new Vector<LockGrant>(10);
	Signature template = null;
	int avoidanceIndex;

	boolean isDisjointUntil(int index, LockGrant lg) {
		for (int i = 0; i < index; i++) {			
			if (!lockGrants.get(i).disjoint(lg))
				return false;
		}						
		return true;
	}
	
	void set(int i, LockGrant lg) {
		lockGrants.set(i, lg);
	}
	
	int size() {
		return template.size();
	}
	
	boolean contains(LockGrant lg) {
		for (int i = 0; i < this.size(); i++) {
			if (lockGrants.get(i) == lg)
				return true;
		}
		return false;
	}
	
	boolean isActive(ThreadNode t) {
		for (int i = 0; i < this.size(); i++) {
			if (lockGrants.get(i).thread != t && lockGrants.get(i).n == 0)
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			sb.append("("+ lockGrants.get(i).thread+ ",");
			sb.append(lockGrants.get(i).lock+ ",");
			sb.append(lockGrants.get(i).n+ ",");
			sb.append(lockGrants.get(i).position + ") ");
		}
		return sb.toString();
	}
	
	void setSize(int newSize) {
		this.lockGrants.setSize(newSize);
	}
	
	void clear() {
		this.lockGrants.clear();
		this.template = null;
	}
	
	boolean isEmpty() {
		return this.template == null;
	}	
}
