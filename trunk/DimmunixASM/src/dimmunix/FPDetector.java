package dimmunix;

import java.util.HashSet;

public class FPDetector {
	Dimmunix dImmunix;
	
	Vector<YieldEvent> currentYields = new Vector<YieldEvent>(10000);
	HashSet<ThreadNode> avoidanceThreads = new HashSet<ThreadNode>(2048);
	
	public static int nTPs = 0;
	public static int nFPs = 0;
	
	public FPDetector(Dimmunix dImmunix) {
		this.dImmunix = dImmunix;
	}

	void processEvent(Event evt) {
		if (evt.type == EventType.REQUEST) {
			evt.thread.lockOps.add(evt);
		}
		else if (evt.type == EventType.YIELD) {
			YieldEvent yEvt = (YieldEvent)evt;
			
			if (yEvt.isTP())
				nTPs++;
			else
				nFPs++;
			
			SignaturePosition tpos = yEvt.matchingTemplate.positions.get(yEvt.avoidanceIndex());
			if (yEvt.matchingTemplate.isDeadlockTemplate && tpos.depth < dImmunix.maxCallStackDepth && tpos.depth < tpos.value.size())
				currentYields.add(yEvt);			
		}
		else if (evt.type == EventType.RELEASE) {
			evt.thread.lockOps.add(evt);
			
			YieldEvent yEvt = endsAvoidance(evt);
			if (yEvt != null) {
				SignaturePosition tpos = yEvt.matchingTemplate.positions.get(yEvt.avoidanceIndex());
				
				if (tpos.depth >= dImmunix.maxCallStackDepth || tpos.depth >= tpos.value.size()) {
					currentYields.remove(yEvt);
					return;
				}
				
				if (yEvt.yields.get(yEvt.avoidanceIndex()).position.size() == tpos.depth) {
					if (isTruePositive(yEvt, evt.time)) {
						tpos.nTPs++;
					}
					else {
						tpos.nFPs++;
					}
					if (tooManyFPs(tpos)) {					
						tpos.incrementPrecision();
						tpos.nFPs = 0;
						tpos.nTPs = 0;
					}					
				}
				
				currentYields.remove(yEvt);				
			}
		}
	}
	
	boolean tooManyFPs(SignaturePosition tpos) {
		int threshold_yields = 20;
		int threshold_fps = 2;
		return (tpos.nFPs+ tpos.nTPs >= threshold_yields) && (tpos.nFPs >= threshold_fps);
	}
	
	YieldEvent endsAvoidance(Event rEvt) {
		for (int i = 0; i < currentYields.size(); i++) {
			YieldEvent yEvt = currentYields.get(i); 
			if (yEvt.thread == rEvt.thread && (!dImmunix.ignoreLocksInAvoidance && yEvt.lock == rEvt.lock || dImmunix.ignoreLocksInAvoidance && yEvt.position == rEvt.position))
				return yEvt;
		}
		return null;
	}
	
	boolean isTruePositive(YieldEvent yEvt, long tRelease) {
		for (int i = 0; i < yEvt.yields.size(); i++) {
			if (!isTruePositive(yEvt, tRelease, i))
				return false;
		}
		return true;
	}
	
	boolean isTruePositive(YieldEvent yEvt, long tRelease, int k) {
		ThreadNode t_k = yEvt.yields.get(k).thread;
		LockNode l_k = yEvt.yields.get(k).lock;
		long time_k = yEvt.yields.get(k).time;
		LockNode nextLock = yEvt.yields.get((k+ 1)% yEvt.yields.size()).lock;
		
		for (int i = 0; i < t_k.lockOps.size(); i++) {
			Event op = t_k.lockOps.get(i);
			if (op.type == EventType.REQUEST && op.lock == nextLock && op.time > time_k && op.time < tRelease)
				return true;
			if (op.type == EventType.RELEASE && op.lock == l_k && op.time > time_k && op.time < tRelease)
				return false;
		}
		return false;
	}
	
	void compactifyTrace() {
		avoidanceThreads.clear();
		long tmin = System.nanoTime();
		
		for (int i = 0; i < currentYields.size(); i++) {
			YieldEvent yEvt = currentYields.get(i);
			for (int j = 0; j < yEvt.yields.size(); j++) {
				avoidanceThreads.add(yEvt.yields.get(j).thread);
				if (yEvt.yields.get(j).time < tmin)
					tmin = yEvt.yields.get(j).time; 
			}
		}
		
		for (ThreadNode t: avoidanceThreads)
			for (int i = 0; i < t.lockOps.size() && t.lockOps.get(i).time < tmin; i++)
				t.lockOps.remove(i--);
	}
}
