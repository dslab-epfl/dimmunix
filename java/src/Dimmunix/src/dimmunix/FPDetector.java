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

package dimmunix;

import java.util.HashSet;

public class FPDetector {
	Dimmunix dimmunix;
	
	Vector<YieldEvent> currentYields = new Vector<YieldEvent>(10000);
	HashSet<ThreadNode> avoidanceThreads = new HashSet<ThreadNode>(2048);
	
	public static int nTPs = 0;
	public static int nFPs = 0;
	
	public FPDetector(Dimmunix dImmunix) {
		this.dimmunix = dImmunix;
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
			if (yEvt.matchingTemplate.isDeadlockTemplate && tpos.depth < dimmunix.maxCallStackDepth && tpos.depth < tpos.value.size())
				currentYields.add(yEvt);			
		}
		else if (evt.type == EventType.RELEASE) {
			evt.thread.lockOps.add(evt);
			
			YieldEvent yEvt = endsAvoidance(evt);
			if (yEvt != null) {
				SignaturePosition tpos = yEvt.matchingTemplate.positions.get(yEvt.avoidanceIndex());
				
				if (isTruePositive(yEvt, evt.time)) {
					tpos.nTPs++;
				}
				else {
					tpos.nFPs++;
				}
				
				if (tpos.depth >= dimmunix.maxCallStackDepth || tpos.depth >= tpos.value.size()) {
					if (tooManyFPs(yEvt.matchingTemplate)) {
						dimmunix.history.remove(yEvt.matchingTemplate);
					}
				}
				else {
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
		int threshold_fps = 10;
		return (tpos.nFPs+ tpos.nTPs >= threshold_yields) && (tpos.nFPs >= threshold_fps);
	}
	
	boolean tooManyFPs(Signature sig) {
		int threshold_yields = 100;
		int threshold_fps = 100;
		
		int nyields = 0;
		int nfps = 0;
		
		for (SignaturePosition spos: sig.positions) {
			nyields = nyields+ spos.nFPs+ spos.nTPs;
			nfps = nfps+ spos.nFPs;
		}
		
		return nyields >= threshold_yields && nfps >= threshold_fps;
	}
	
	YieldEvent endsAvoidance(Event rEvt) {
		for (int i = 0; i < currentYields.size(); i++) {
			YieldEvent yEvt = currentYields.get(i); 
			if (yEvt.thread == rEvt.thread && (!dimmunix.ignoreLocksInAvoidance && yEvt.lock == rEvt.lock || dimmunix.ignoreLocksInAvoidance && yEvt.position == rEvt.position))
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
