package dimmunix;

public class YieldEvent extends Event {
	Signature matchingTemplate = null;
	Vector<Yield> yields = new Vector<Yield>(10);
	
	void clearYields() {
		yields.elementCount = 0;
	}
	
	void addYield(ThreadNode t, LockNode l, Position p, long time) {
		yields.add(new Yield(t, l, p, time));
	}
	
	int avoidanceIndex() {
		for (int i = 0; i < yields.size(); i++) {
			if (yields.get(i).thread == thread)
				return i;
		}
		return -1;
	}
	
	boolean isTP() {
		for (int i = 0; i < yields.size(); i++) {
			if (!yields.get(i).position.equals(matchingTemplate.positions.get(i).value))
				return false;
		}
		return true;
	}
}
