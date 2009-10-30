package dimmunix;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventProcessor extends Thread {	
	Dimmunix dimmunix;
	
	public ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<Event>();
	
	CycleDetector cycleDetector;
	Logger logger;
	FPDetector fpDetector;
	
	int processingTimeoutMsec;
	
	ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
	
	public EventProcessor(Dimmunix dImmunix) {
		this.setName("Event Processor");
		this.dimmunix = dImmunix;
		
		if (dImmunix.experimentLevel == Dimmunix.FULL)
			this.cycleDetector = new CycleDetector(dImmunix);
		else
			this.cycleDetector = null;
		
		this.logger = new Logger(dImmunix);
		if (dImmunix.adjustPrecision)
			this.fpDetector = new FPDetector(dImmunix);
		
		this.processingTimeoutMsec = 5* dImmunix.cycleDetectionPeriodMsec;
	}
	
	Event getNewEvent(long time, EventType evtType, ThreadNode t, LockNode l, Position p, boolean posInHist, Instance instance) {
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
		evt.posInHistory = posInHist;
		if (evtType == EventType.YIELD) {
			((YieldEvent)evt).matchingTemplate = instance.template;
			for (int i = 0; i < instance.size(); i++) {
				LockGrant lg = instance.lockGrants.get(i);
				((YieldEvent)evt).addYield(lg.thread, lg.lock, lg.position, lg.time);
			}			
		}
		return evt;
	}
	
	void addRequestEvent(ThreadNode t, LockNode l, Position p, boolean posInHist) {
		this.events.add(getNewEvent(System.nanoTime(), EventType.REQUEST, t, l, p, posInHist, null));
	}
	
	void addAcquireEvent(ThreadNode t, LockNode l, Position p, boolean posInHist) {
		this.events.add(getNewEvent(0, EventType.ACQUIRE, t, l, p, posInHist, null));
	}
	
	void addReleaseEvent(ThreadNode t, LockNode l, Position p, boolean posInHist) {
		this.events.add(getNewEvent(System.nanoTime(), EventType.RELEASE, t, l, p, posInHist, null));
	}
	
	void addYieldEvent(ThreadNode t, LockNode l, Position p, Instance instance) {
		this.events.add(getNewEvent(0, EventType.YIELD, t, l, p, true, instance));
	}

	void addGrantEvent(ThreadNode t, LockNode l, Position p, boolean posInHist) {
		this.events.add(getNewEvent(0, EventType.GRANT, t, l, p, posInHist, null));
	}
	
	void addWakeUpEvent(ThreadNode t, LockNode l, Position p) {
		this.events.add(getNewEvent(0, EventType.WAKE_UP, t, l, p, true, null));
	}
	
	void processEvents(boolean withTimeout) {
		Event evt;
		long t0 = System.nanoTime();
		while ((evt = events.poll()) != null) {
			if (this.cycleDetector != null)
				cycleDetector.processEvent(evt);
			
			logger.processEvent(evt);
			
			if (this.fpDetector != null)
				fpDetector.processEvent(evt);
						
			if (withTimeout) {
				int tElapsed = (int)((System.nanoTime()- t0)/ 1000/ 1000);
				if (tElapsed > processingTimeoutMsec)
					break;				
			}
		}
	}
	
	boolean allAppThreadsDead() {
		for (Thread t: dimmunix.appThreads) {
			if (t.isAlive())
				return false;
		}
		return true;
	}
	
	void session(boolean withTimeout) {
		this.processEvents(withTimeout);
		
		if (cycleDetector != null)
			cycleDetector.checkForCycles();			
		
		logger.refreshStats();
	
		if (fpDetector != null) 
			fpDetector.compactifyTrace();
	}
	
	boolean mainStarted() {
		return threadBean.getTotalStartedThreadCount() > 5;
	}
	
	public void run() {
		while (!this.mainStarted()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}			
		}
		while (threadBean.getThreadCount() > 5) {
			try {
				Thread.sleep(dimmunix.cycleDetectionPeriodMsec);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			session(true);
			if (dimmunix.saveLogPeriodically) {
				logger.updateLog();
			}
		}
		session(false);
		logger.updateLog();
		dimmunix.refreshHistory();
	}		
}
