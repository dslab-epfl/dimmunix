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

package dimmunix.deadlock;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventProcessor extends Thread {	
	DimmunixDeadlock dimmunix;
	
	public ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<Event>();
	
	public CycleDetector cycleDetector;
	
	int processingTimeoutMsec;
	public volatile boolean running;
	
	public EventProcessor(DimmunixDeadlock dimmunix) {
		this.setName("Event Processor");
		this.dimmunix = dimmunix;
		
		if (dimmunix.experimentLevel == DimmunixDeadlock.FULL)
			this.cycleDetector = new CycleDetector(dimmunix);
		else
			this.cycleDetector = null;
		
		this.processingTimeoutMsec = 5* dimmunix.cycleDetectionPeriodMsec;
		this.setDaemon(true);
	}
	
	Event getNewEvent(long time, EventType evtType, ThreadNode t, LockNode l, Position p, Instance instance) {
		Event evt = null;
		if (evtType == EventType.YIELD)
			evt = new YieldEvent();
		else
			evt = new Event();		
		evt.time = time;
		evt.type = evtType;
		evt.thread = t;
		evt.lock = l;
		evt.position = p;
		if (evtType == EventType.YIELD) {
			YieldEvent yevt = (YieldEvent)evt; 
			yevt.matchingTemplate = instance.template;
			for (LockGrant lg: instance.lockGrants) {
				((YieldEvent)evt).addYield(lg.thread, lg.lock, lg.position);
			}			
		}
		return evt;
	}
	
	void addRequestEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.REQUEST, t, l, p, null));
	}
	
	void addAcquireEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.ACQUIRE, t, l, p, null));
	}
	
	void addReleaseEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.RELEASE, t, l, p, null));
	}
	
	void addYieldEvent(ThreadNode t, LockNode l, Position p, Instance instance) {
		this.events.add(getNewEvent(0, EventType.YIELD, t, l, p, instance));
	}

	void addYieldSimpleEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.YIELD_SIMPLE, t, l, p, null));
	}

	void addGrantEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.GRANT, t, l, p, null));
	}
	
	void addGrantSimpleEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.GRANT_SIMPLE, t, l, p, null));
	}
	
	void addWakeUpEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.WAKE_UP, t, l, p, null));
	}
	
	void processEvents(boolean withTimeout) {
		Event evt;
		long t0 = System.nanoTime();
		while ((evt = events.poll()) != null) {
			if (this.cycleDetector != null)
				cycleDetector.processEvent(evt);
			
			if (withTimeout) {
				int tElapsed = (int)((System.nanoTime()- t0)/ 1000/ 1000);
				if (tElapsed > processingTimeoutMsec)
					break;				
			}
		}
	}
		
	void session(boolean withTimeout) {
		this.processEvents(withTimeout);
		
		if (cycleDetector != null) {
			dimmunix.rag.update();
			cycleDetector.checkForYieldCycles();
		}		
	}
	
	public void run() {
		this.running = true;
		while (this.running) {
			try {
				Thread.sleep(dimmunix.cycleDetectionPeriodMsec);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			session(true);
		}
		session(false);
		dimmunix.refreshHistory();
	}		
}
