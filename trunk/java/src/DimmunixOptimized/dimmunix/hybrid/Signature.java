package dimmunix.hybrid;

import java.util.StringTokenizer;

import dimmunix.Util;
import dimmunix.Vector;

public class Signature {
	private class Position {
		StackTraceElement outerPos;
		StackTraceElement innerPos;
		Vector<ThreadNode> allowed = new Vector<ThreadNode>();
		
		Position(StackTraceElement outerPos, StackTraceElement innerPos) {
			this.outerPos = outerPos;
			this.innerPos = innerPos;
		}		
	}
	
	private Vector<Position> positions = new Vector<Signature.Position>();
	
	public Signature(Vector<StackTraceElement> outerPositions, Vector<StackTraceElement> innerPositions) {
		for (int i = 0; i < outerPositions.size(); i++) {
			Position p = new Position(outerPositions.get(i), innerPositions.get(i));
			this.positions.add(p);
		}
	}
	
	public Signature(String sigStr) {
		StringTokenizer stok = new StringTokenizer(sigStr, ",");
		while (stok.hasMoreTokens()) {
			StackTraceElement outerPos = Util.parsePosition(stok.nextToken());
			StackTraceElement innerPos = Util.parsePosition(stok.nextToken());
			Position p = new Position(outerPos, innerPos);
			this.positions.add(p);
		}
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer();
		for (Position p: this.positions) {
			if (str.length() > 0) {
				str.append(',');
			}
			str.append(p.outerPos+ ","+ p.innerPos);
		}
		
		return str.toString();
	}
	
	public boolean enter(ThreadNode t, StackTraceElement pos) {
		boolean matched = false;
		for (Position pout: this.positions) {
			if (pout.outerPos.equals(pos)) {
				pout.allowed.add(t);
				matched = true;
			}
		}
		return matched;
	}
	
	public void exit(ThreadNode t, StackTraceElement pos) {
		for (Position pout: this.positions) {
			if (pout.outerPos.equals(pos)) {
				pout.allowed.remove(t);
			}
		}
	}
	
	public boolean isInstantiated(ThreadNode t) {
		if (this.positions.size() == 2) {
			for (ThreadNode t1: this.positions.get(0).allowed) {
				for (ThreadNode t2: this.positions.get(1).allowed) {
					if (t1 != t2 && (t == t1 || t == t2)) {
						t2.addInstancePosition(this.positions.get(1).outerPos);
						return true;
					}
				}				
			}
			return false;
		}
		if (this.positions.size() == 3) {
			for (ThreadNode t1: this.positions.get(0).allowed) {
				for (ThreadNode t2: this.positions.get(1).allowed) {
					for (ThreadNode t3: this.positions.get(2).allowed) {
						if (t1 != t2 && t1 != t3 && t2 != t3 && (t == t1 || t == t2 || t == t3)) {
							t2.addInstancePosition(this.positions.get(1).outerPos);
							t3.addInstancePosition(this.positions.get(2).outerPos);
							return true;
						}
					}				
				}				
			}
			return false;
		}
		if (this.positions.size() == 4) {
			for (ThreadNode t1: this.positions.get(0).allowed) {
				for (ThreadNode t2: this.positions.get(1).allowed) {
					for (ThreadNode t3: this.positions.get(2).allowed) {
						for (ThreadNode t4: this.positions.get(3).allowed) {
							if (t1 != t2 && t1 != t3 && t1 != t4 && t2 != t3 && t2 != t4 && t3 != t4 && (t == t1 || t == t2 || t == t3 || t == t4)) {
								t2.addInstancePosition(this.positions.get(1).outerPos);
								t3.addInstancePosition(this.positions.get(2).outerPos);
								t4.addInstancePosition(this.positions.get(3).outerPos);
								return true;
							}
						}				
					}				
				}				
			}
			return false;
		}
		//this should not happen in a real program
		return false;
	}	
}
