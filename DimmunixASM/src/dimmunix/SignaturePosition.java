package dimmunix;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SignaturePosition {
	Position value;
	volatile int depth;
	Dimmunix dImmunix;
	
	Vector<Position> matchingPositions = new Vector<Position>();
	ReentrantReadWriteLock rwLockMatching = new ReentrantReadWriteLock();
	
	int nFPs = 0;
	int nTPs = 0;
	
	public SignaturePosition(Position value, int depth, Dimmunix dImmunix) {
		this.value = value;
		this.depth = depth;
		this.dImmunix = dImmunix;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SignaturePosition))
			return false;
		SignaturePosition tpos = ((SignaturePosition)obj);
		return value == tpos.value;
	}

	@Override
	public String toString() {
		return depth+ "#"+ value;
	}		
	
	boolean match(Position pos) {
		if (depth > pos.size())
			return false;
		for (int i = 0; i < depth; i++) {
			if (!value.callStack.get(i).equals(pos.callStack.get(i)))
				return false;
		}
		return true;
	}
	
	void incrementPrecision() {
		depth++;
		refreshExistingMatchingPositions();
	}
	
	void decrementPrecision() {
		depth--;
		refreshMatchingPositionsInHist();
	}
	
	void refreshMatchingPositionsInHist() {
		rwLockMatching.writeLock().lock();
		
		matchingPositions.clear();		
		matchingPositions.add(this.value);
		for (Signature tmpl: dImmunix.history) {
			for (int i = 0; i < tmpl.size(); i++) {
				Position pos = tmpl.positions.get(i).value;
				if (!matchingPositions.containsRef(pos) && this.match(pos))
					matchingPositions.add(pos);
			}
		}
		
		rwLockMatching.writeLock().unlock();
	}
	
	void refreshExistingMatchingPositions() {
		rwLockMatching.writeLock().lock();
		
		for (int i = 0; i < matchingPositions.size(); i++) {
			if (!this.match(matchingPositions.get(i)))
				matchingPositions.remove(i--);
		}
		
		rwLockMatching.writeLock().unlock();
	}	
	
	void getMatchingPositions(Vector<Position> positions) {
		rwLockMatching.readLock().lock();
		matchingPositions.copyInto(positions);
		rwLockMatching.readLock().unlock();
	}	
	
	void matchAndAdd(Position p) {
		rwLockMatching.writeLock().lock();
		if (match(p))
			matchingPositions.add(p);
		rwLockMatching.writeLock().unlock();		
	}
}
