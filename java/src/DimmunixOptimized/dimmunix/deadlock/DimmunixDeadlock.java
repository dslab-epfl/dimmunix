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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import communix.Frame;

import dimmunix.CallStack;
import dimmunix.Pair;
import dimmunix.SpecialStackTrace;
import dimmunix.Vector;
import dimmunix.analysis.SkipAvoidance;
import dimmunix.analysis.SkipAvoidanceAnalysis;

public class DimmunixDeadlock {

	public static final DimmunixDeadlock instance = new DimmunixDeadlock();

	public static final int NOTHING = 0;
//	public static final int EVENTS0 = 1;
	public static final int LOOKUP = 2;
//	public static final int EVENTS = 3;
	public static final int UPDATES = 4;
	public static final int AVOIDANCE = 5;
	public static final int FULL = 6;

	public int experimentLevel = FULL;
	boolean ignoreLocksInAvoidance = false;
	int cycleDetectionPeriodMsec = 100;// msec
	boolean adjustPrecision = true;// automatically adjusting match depths
	boolean enableDynamicMatchDepth = true;
	boolean collaborativeYield = true;
	boolean ignoreAvoidance = false;
	int yieldTimeout = 0;
	
	int maxCallStackDepth = 10;
	
	boolean simpleAvoidance = false;
	public boolean inlineMatching = true;

	final String appName = "Dimmunix";

	public ResourceAllocationGraph rag;
	
	public History history = new History();

	// ConcurrentLinkedQueue<Thread> appThreads = new
	// ConcurrentLinkedQueue<Thread>();

	final String histFile = appName + ".hist";
	final String logFile = appName + ".log";
	final String confFile = appName + ".conf";

	public EventProcessor eventProcessor;
	
	public volatile boolean newCyclesFound = false; 

	int callStackOffset = 5;
	int yieldCallStackOffset = 3;

	long startTime;
	AtomicInteger nSyncs = new AtomicInteger(0);
	
	Field syncFieldReentrantLock;
	
	public AtomicInteger nYields = new AtomicInteger(0);

	// <-communix
	int maxFPs = Integer.MAX_VALUE;

	// ->

	DimmunixDeadlock() {
		if (System.getProperty("java.runtime.version").substring(0, 3).equals("1.6"))
			callStackOffset = 4;

		this.rag = new ResourceAllocationGraph(this);

		try {
			syncFieldReentrantLock = ReentrantLock.class.getDeclaredField("sync");
			syncFieldReentrantLock.setAccessible(true);
		} catch (SecurityException e) {
			syncFieldReentrantLock = null;
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			syncFieldReentrantLock = null;
			e.printStackTrace();
		}
	}

	public void init() {
		this.loadConfiguration();

		this.loadHistory();

		this.eventProcessor = new EventProcessor(this);		
		this.eventProcessor.start();

		startTime = System.nanoTime();
	}

	private void parseExperimentLevel(String val) {
		if (val.equals("NOTHING"))
			experimentLevel = NOTHING;
		else if (val.equals("LOOKUP"))
			experimentLevel = LOOKUP;
//		else if (val.equals("EVENTS0"))
//			experimentLevel = EVENTS0;
//		else if (val.equals("EVENTS"))
//			experimentLevel = EVENTS;
		else if (val.equals("UPDATES"))
			experimentLevel = UPDATES;
		else if (val.equals("AVOIDANCE"))
			experimentLevel = AVOIDANCE;
		else if (val.equals("FULL"))
			experimentLevel = FULL;
	}

	String toStringExperimentLevel() {
		switch (experimentLevel) {
		case NOTHING:
			return "NOTHING";
		case LOOKUP:
			return "LOOKUP";
//		case EVENTS:
//			return "EVENTS";
//		case EVENTS0:
//			return "EVENTS0";
		case UPDATES:
			return "UPDATES";
		case AVOIDANCE:
			return "AVOIDANCE";
		default:
			return "FULL";
		}
	}

	void loadConfiguration() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(confFile));
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				StringTokenizer stok = new StringTokenizer(line, "= ");
				String key = stok.nextToken();
				String val = stok.nextToken();
				if (key.equals("experimentLevel"))
					this.parseExperimentLevel(val);
				else if (key.equals("maxCallStackDepth"))
					maxCallStackDepth = Integer.parseInt(val);
				else if (key.equals("ignoreLocksInAvoidance"))
					ignoreLocksInAvoidance = Boolean.parseBoolean(val);
				else if (key.equals("collaborativeYield"))
					collaborativeYield = Boolean.parseBoolean(val);
				else if (key.equals("cycleDetectionPeriodMsec"))
					cycleDetectionPeriodMsec = Integer.parseInt(val);
				else if (key.equals("adjustPrecision"))
					adjustPrecision = Boolean.parseBoolean(val);
				else if (key.equals("ignoreAvoidance"))
					ignoreAvoidance = Boolean.parseBoolean(val);
				else if (key.equals("simpleAvoidance"))
					simpleAvoidance = Boolean.parseBoolean(val);
				else if (key.equals("inlineMatching"))
					inlineMatching = Boolean.parseBoolean(val);
			}
			br.close();
		} catch (Exception e) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(confFile));
				bw.write("experimentLevel = " + toStringExperimentLevel() + System.getProperty("line.separator"));
				bw.write("maxCallStackDepth = " + maxCallStackDepth + System.getProperty("line.separator"));
				bw.write("ignoreLocksInAvoidance = " + ignoreLocksInAvoidance + System.getProperty("line.separator"));
				bw.write("collaborativeYield = " + collaborativeYield + System.getProperty("line.separator"));
				bw.write("cycleDetectionPeriodMsec = " + cycleDetectionPeriodMsec + System.getProperty("line.separator"));
				bw.write("adjustPrecision = " + adjustPrecision + System.getProperty("line.separator"));
				bw.write("ignoreAvoidance = " + ignoreAvoidance + System.getProperty("line.separator"));
				bw.write("simpleAvoidance = " + simpleAvoidance + System.getProperty("line.separator"));
				bw.write("inlineMatching = " + inlineMatching + System.getProperty("line.separator"));
				bw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	void loadHistory() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(histFile));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (line.equals(""))
					continue;
				StringTokenizer lineTok = new StringTokenizer(line, "=");
				boolean isDeadlockSig = lineTok.nextToken().equals("deadlock_template");
				StringTokenizer templTok = new StringTokenizer(lineTok.nextToken(), ";");
				Signature templ = new Signature(isDeadlockSig, Integer.parseInt(templTok.nextToken()));

				while (templTok.hasMoreTokens()) {
					StringTokenizer posTok = new StringTokenizer(templTok.nextToken(), "#");
					int depth = Integer.parseInt(posTok.nextToken());

					StringTokenizer stackTok = new StringTokenizer(posTok.nextToken(), ",");
					CallStack callStack = new CallStack(maxCallStackDepth);
					int d = 0;
					while (stackTok.hasMoreTokens() && d++ < maxCallStackDepth)
						callStack.add(stackTok.nextToken());
					templ.add(new SignaturePosition(rag.getPosition(callStack), depth, templ, templ.size()));

					InnerPosition innerPos = new InnerPosition();
					try {
						StringTokenizer innerStackTok = new StringTokenizer(posTok.nextToken(), ",");
						d = 0;
						while (innerStackTok.hasMoreTokens() && d++ < maxCallStackDepth)
							innerPos.add(innerStackTok.nextToken());
						templ.addInner(innerPos);
					} catch (NoSuchElementException e) {
					}
				}
				history.add(templ);
			}
			br.close();
		} catch (FileNotFoundException e) {
			try {
				new File(histFile).createNewFile();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public Signature toDimmunixSignature(communix.Signature sig0) {
		Signature sig = new Signature(true);
		
		for (communix.CallStack cs0: sig0.outerStacks) {
			CallStack cs = new CallStack(this.maxCallStackDepth);
			for (Frame f0: cs0.frames) {
				cs.add(f0.frame);
			}
			sig.add(new SignaturePosition(this.rag.getPosition(cs), 1, sig, sig.size()));
		}
		for (communix.CallStack cs0: sig0.innerStacks) {
			InnerPosition cs = new InnerPosition();
			for (Frame f0: cs0.frames) {
				cs.add(f0.frame);
			}
			sig.addInner(cs);
		}
		
		return sig;
	}

	boolean instantiatesTemplates(ThreadNode t, LockNode l) {
		for (SignaturePosition pos : t.currentMatchingPositions) {
			if (this.instance(pos.signature, pos.index, t, l))
				return true;
		}
		return false;
	}

	boolean instance(Signature templ, int index, ThreadNode t, LockNode l) {
		t.currentLockGrantsIterators.setSize(templ.size());
		t.curInstance.setSize(templ.size());
		t.curInstance.template = templ;

		LockGrant lgIndex = t.findLockGrant(l, templ.positions.get(index).value);
		t.curInstance.set(index, lgIndex);

		int sigSize = templ.size();
		for (int i = 0; i < sigSize; i++) {
			if (i != index) {
				t.currentLockGrantsIterators.set(i, templ.positions.get(i).value.lockGrants.iterator());					
			}
		}

		int k = 0;
		while (k >= 0) {
			if (k == sigSize) {
				if (Instance.contains(t.instancesToSkip, t.curInstance))
					return false;
				return true;
			}
			if (k == index) {
				if (t.curInstance.isDisjointUntil(k, lgIndex))
					k++;
				else
					k--;
				continue;
			}

			Iterator<LockGrant> lockGrants = t.currentLockGrantsIterators.get(k);

			boolean found = false;
			while (lockGrants.hasNext()) {
				LockGrant lg = lockGrants.next();
//				if (templ.threadsToSkip.contains(lg.thread))
				if (lg.lock.skip)
					continue;
				if (t.curInstance.isDisjointUntil(k, lg)) {
					t.curInstance.set(k, lg);
					found = true;
					break;
				}
			}

			if (found)
				k++;
			else {
				t.currentLockGrantsIterators.set(k, templ.positions.get(k).value.lockGrants.iterator());					
				k--;
				if (k == index)
					k--;
			}
		}

		return false;
	}

	public void avoidance(Object l, boolean isSyncBlock) {
		if (l == null || experimentLevel == DimmunixDeadlock.NOTHING)
			return;

		if (!isSyncBlock && syncFieldReentrantLock != null) {
			try {
				l = syncFieldReentrantLock.get(l);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		Thread t = Thread.currentThread();

		ThreadNode tnode = rag.getThreadNode(t);

/*		if (isSyncBlock) {
			if (Thread.holdsLock(l) && lnode.owner != tnode) {
				// we missed the acquisition of l, store that l was acquired at
				// least once
				lnode.nlck = 1;
				lnode.owner = tnode;
				// System.out.println("acquisition missed for "+ lnode);
				return;
			}			
		}*/

		if (isSyncBlock && this.inlineMatching) {
			getTemplatesContainingCurrentPosition(tnode, true);	
		}
		else {
			getCallStack(tnode);

			getTemplatesContainingCurrentPosition(tnode, false);

			tnode.reqPos = tnode.currentCallStack;			
		}

		if (!tnode.reqPosInHistory) {
			return;
		}			

		LockNode lnode = rag.getLockNode(l);
		
		if (lnode.owner == tnode) {
			return;
		}

		if (experimentLevel == DimmunixDeadlock.LOOKUP) {
			return;
		}		
		
		if (simpleAvoidance) {
			this.nYields.getAndIncrement();
			
			if (this.experimentLevel >= AVOIDANCE) {		
				this.eventProcessor.addYieldSimpleEvent(tnode, lnode, null);
				for (Signature sig: tnode.currentTemplates) {					
					sig.lock.lockNode.semReqPos = tnode.currentMatchingPositions.get(0).value.callStack;
					tnode.semNext = sig.lock.lockNode;
					if (!this.ignoreAvoidance) {
						sig.lock.acquire();
					}
					tnode.semNext = null;
					sig.lock.lockNode.semNext = tnode;
				}		
				this.eventProcessor.addGrantSimpleEvent(tnode, lnode, null);
			}
			
			return;
		}
		
		while (!request(tnode, lnode)) {
			if (collaborativeYield) {
				LockGrant otherLockGrant = null;
				for (LockGrant lg: tnode.curInstance.lockGrants) {
					if (lg.thread != tnode) {
						lg.yieldersLock.readLock().lock();
						otherLockGrant = lg;
					}
				}
				
				if (tnode.curInstance.isActive(tnode)) {
					// <-communix (line 5 , alg 2)
					if (this.adjustPrecision) {
						Instance curInstCopy = tnode.curInstance.cloneInstance();
						for (LockGrant lg: tnode.curInstance.lockGrants) {
							lg.instances.add(curInstCopy);						
						}							
					}
					// ->
					
					int deadlockSize = tnode.curInstance.template.size();
//					Object yieldLock = (deadlockSize > 2)? tnode.yieldLock: otherLockGrant;
					Object yieldLock = tnode.yieldLock;
					synchronized (yieldLock) {
						for (LockGrant lg: tnode.curInstance.lockGrants) {
							if (lg.thread != tnode) {
//								if (deadlockSize > 2) {
									lg.yielders.add(tnode);
//								}
								lg.yieldersLock.readLock().unlock();
							}
						}

//						System.out.println("yielding");
						eventProcessor.addYieldEvent(tnode, lnode, null, tnode.curInstance);
						this.nYields.getAndIncrement();
						
						if (!ignoreAvoidance) {
							tnode.curInstance.template.currentYields.add(tnode.curInstance);
							try {
								yieldLock.wait(yieldTimeout);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							tnode.curInstance.template.currentYields.remove(tnode.curInstance);
						}
						
						this.eventProcessor.addGrantEvent(tnode, lnode, null);
					}

					// eventProcessor.addWakeUpEvent(tnode, lnode, p);

					if (ignoreAvoidance) {
						if (adjustPrecision) {
							tnode.curInstance.template.positions.get(0).incrementPrecision();
							tnode.curInstance.template.positions.get(1).incrementPrecision();
						}
						
						this.grant(tnode, ignoreLocksInAvoidance ? null : lnode);
						eventProcessor.addGrantEvent(tnode, lnode, null);

						// tnode.bypassAvoidance = false;
						return;
					}
				} else {
					for (LockGrant lg: tnode.curInstance.lockGrants) {
						if (lg.thread != tnode)
							lg.yieldersLock.readLock().unlock();
					}
				}
			} else {
				eventProcessor.addYieldEvent(tnode, lnode, null, tnode.curInstance);
				if (!ignoreAvoidance) {
					Thread.yield();
					// eventProcessor.addWakeUpEvent(tnode, lnode, p);
				} else {
					this.grant(tnode, ignoreLocksInAvoidance ? null : lnode);
					eventProcessor.addGrantEvent(tnode, lnode, null);
					return;
				}
			}
		}

		tnode.instancesToSkip.clear();
	}

	void grant(ThreadNode t, LockNode l) {
		t.currentLockGrants.clear();
		for (SignaturePosition p: t.currentMatchingPositions) {
			t.currentLockGrants.add(p.value.grant(t, l));
		}
	}

	boolean request(ThreadNode t, LockNode l) {
		LockNode lAv = (ignoreLocksInAvoidance) ? null : l;
		if (experimentLevel >= AVOIDANCE) {
			this.grant(t, lAv);
			boolean unsafe = this.instantiatesTemplates(t, lAv);
			if (unsafe) {
				this.ungrant(t.currentLockGrants);
				return false;
			} else {
				return true;
			}
		} else {
			if (experimentLevel >= UPDATES)
				this.grant(t, lAv);
			return true;
		}
	}

	public void acquire(Object l, boolean isSyncBlock) {		
		if (l == null || instance.experimentLevel == DimmunixDeadlock.NOTHING)
			return;
		
		this.nSyncs.getAndIncrement();
		
		if (!isSyncBlock && syncFieldReentrantLock != null) {
			try {
				l = syncFieldReentrantLock.get(l);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}		

		Thread t = Thread.currentThread();

		ThreadNode tnode = this.rag.getThreadNode(t);
		
		LockNode lnode = this.rag.getLockNode(l);
		
		if (experimentLevel == LOOKUP)
			return;		

		if (lnode.owner == null) {

			lnode.owner = tnode;
			
			if (simpleAvoidance) {
				if (tnode.reqPos != null && !isSyncBlock) {
					lnode.acqPos = tnode.reqPos.cloneStack();
				}
				tnode.currentTemplates.copyInto(lnode.matchingTemplates);
				lnode.acqPosInHistory = tnode.reqPosInHistory;
			}
			else {
				if (tnode.reqPos != null && !isSyncBlock) {
					lnode.acqPos = tnode.reqPos.cloneStack();
				}
				if (tnode.reqPosInHistory) {
					tnode.currentMatchingPositions.copyInto(lnode.matchingPositions);
					tnode.currentLockGrants.copyInto(lnode.currentLockGrants);
				}
				lnode.acqPosInHistory = tnode.reqPosInHistory;

				// <-communix (lines 6-9)
				if (this.adjustPrecision) {
					for (LockNode lHeld : tnode.locksHeld) {
						if (lHeld.acqPosInHistory) {
							lHeld.innerLocks.add(lnode);
						}
					}					
				}
				// ->
				tnode.locksHeld.add(lnode);				
			}			
		}
		
		lnode.nlck++;
	}

	public void release(Object l, boolean isSyncBlock) {
		if (l == null || instance.experimentLevel == DimmunixDeadlock.NOTHING)
			return;

		if (!isSyncBlock && syncFieldReentrantLock != null) {
			try {
				l = syncFieldReentrantLock.get(l);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		Thread t = Thread.currentThread();

		ThreadNode tnode = this.rag.getThreadNode(t);
		
		LockNode lnode = this.rag.getLockNode(l);
		
		if (experimentLevel == LOOKUP)
			return;

		if (lnode.owner != tnode)
			return;

		if (--lnode.nlck == 0) {

			lnode.owner = null;
			lnode.acqPos = null;
			
			if (simpleAvoidance) {
				if (lnode.acqPosInHistory && this.experimentLevel >= AVOIDANCE) {
					for (Signature sig: lnode.matchingTemplates) {
						sig.lock.lockNode.semNext = null;
						if (!this.ignoreAvoidance) {
							sig.lock.release();							
						}
					}					
				}
			}
			else {
				lnode.skip = false;
				tnode.locksHeld.remove(lnode);

				if (lnode.acqPosInHistory) {
					for (LockGrant lg: lnode.currentLockGrants) {
						this.ungrant(lg);
					}
					lnode.currentLockGrants.clear();
					lnode.matchingPositions.clear();
				}				
			}			
		}
	}

	// <-communix
	// Algorithm 3
	void checkAvoidance(LockGrant lg, boolean lIsReleased) {
		ThreadNode t = lg.thread;
		LockNode l = lg.lock;
		
		HashSet<LockNode> innerLCopy = new HashSet<LockNode>(l.innerLocks);
		for (Instance inst: lg.instances) {	
			int indexL = inst.indexOf(l);
			if (indexL != -1 && t == inst.lockGrants.get(indexL).thread) {
				inst.locksHeldArray.set(indexL, innerLCopy);
			}
		}
		
		if (!lIsReleased)
			return;
		
		for (Instance inst : lg.instances) {
			// check whether it is TP
			if (inst.avoidanceIsDone()) {
				if (inst.isTruePositive()) {
					inst.template.nTP.getAndIncrement();		
				}
				else {
					inst.template.nFP.getAndIncrement();
					if (this.adjustPrecision) {
						inst.template.positions.get(0).incrementPrecision();
						inst.template.positions.get(1).incrementPrecision();							
					}
					if (inst.template.nFP.get() > this.maxFPs && inst.template.nTP.get() == 0) {
						this.history.remove(inst.template);
						this.refreshHistory();
					}					
				}
			}
		}
		
		l.innerLocks.clear();
		
		lg.instances.clear();
	}

	// ->

	void ungrant(Vector<LockGrant> lgs) {
		for (LockGrant lg: lgs) {
			if (lg.n == 1 && collaborativeYield && experimentLevel >= AVOIDANCE)
				lg.yieldersLock.writeLock().lock();
			// if (lg.n == 1)
			// this.eventProcessor.addReleaseEvent(lg.thread, lg.lock, lg.position,
			// lg.lock.acqPosInHistory);
			int n = lg.position.ungrant(lg);
			if (n == 0 && collaborativeYield && experimentLevel >= AVOIDANCE) {
				this.wakeUpYieldingThreads(lg);
				lg.yieldersLock.writeLock().unlock();
			}
		}
	}

	void ungrant(LockGrant lg) {
		if (lg.n == 1 && collaborativeYield && experimentLevel >= AVOIDANCE)
			lg.yieldersLock.writeLock().lock();

		// <-communix
		if (lg.n == 1 && this.adjustPrecision) {
			checkAvoidance(lg, true);								
		}
		// ->

		int n = lg.position.ungrant(lg);
		if (n == 0 && collaborativeYield && experimentLevel >= AVOIDANCE) {
			this.wakeUpYieldingThreads(lg);
			lg.yieldersLock.writeLock().unlock();
		}
	}

	void wakeUpYieldingThreads(LockGrant lg) {
		if (!ignoreAvoidance) {
			synchronized (lg) {
				lg.notifyAll();
			}
			for (ThreadNode yt : lg.yielders) {
				synchronized (yt.yieldLock) {
					yt.yieldLock.notify();
				}
			}
		}
		lg.yielders.clear();
	}

	void getCallStack(ThreadNode t) {
//		StackTraceElement[] trace = t.thread.getStackTrace();
		SpecialStackTrace.getStackTrace(t.currentStackTrace, maxCallStackDepth+ callStackOffset, callStackOffset);

		t.currentCallStack.clear();
		for (int depth = 0; depth < t.currentStackTrace.size() && depth < maxCallStackDepth; depth++)
			t.currentCallStack.add(t.currentStackTrace.get(depth));
	}

	void getTemplatesContainingCurrentPosition(ThreadNode t, boolean fast) {
		t.reqPosInHistory = false;
		t.currentTemplates.clear();
		t.currentMatchingPositions.clear();
		for (int s = 0; s < this.history.sigIdMax+ 1; s++) {
			if (this.history.historyMap[s] == null)
				continue;
			Signature sig = this.history.historyMap[s];
			int nPositions = sig.positions.size();
			for (int i = 0; i < nPositions; i++) {
				SignaturePosition pos_i = sig.positions.get(i); 
				if (!fast && pos_i.match(t.currentCallStack) || fast && pos_i.matches[t.id] == 0) {
					if (!t.currentTemplates.containsRef(sig))
						t.currentTemplates.add(sig);
					t.currentMatchingPositions.add(pos_i);
					t.reqPosInHistory = true;
					if (fast) {
						//reset counter
						pos_i.matches[t.id] = Integer.MAX_VALUE;
					}
				}
			}
		}
	}

	void refreshHistory() {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(instance.histFile, false));
			for (Signature tmpl : instance.history.historyQueue)
				bw.write(tmpl + System.getProperty("line.separator"));
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void skipAvoidance(int sigId, int index) {
//		System.out.println("skiping avoidance for signature " + sigId);

		ThreadNode tnode = DimmunixDeadlock.instance.rag.getThreadNode(Thread.currentThread());

		DimmunixDeadlock.instance.skipAvoidance(DimmunixDeadlock.instance.history.historyMap[sigId], index, tnode);		
	}

	void skipAvoidance(Signature sig, int index, ThreadNode t) {
		if (this.simpleAvoidance)
			return;
		SignaturePosition sigPos = sig.positions.get(index);
		for (LockNode l: t.locksHeld) {
			if (!l.acqPosInHistory)
				continue;
			for (SignaturePosition p: l.matchingPositions) {
				if (sigPos.value == p.value) {
					l.skip = true;
					break;
				}
			}
		}

		for (Instance inst : sig.currentYields) {
			for (LockGrant lg : inst.lockGrants) {
				if (lg.thread == t) {
					lg.yieldersLock.writeLock().lock();
					if (this.adjustPrecision) {
						this.checkAvoidance(lg, false);						
					}
					this.wakeUpYieldingThreads(lg);
					lg.yieldersLock.writeLock().unlock();
				}
			}
		}
	}

	public void shutDown() {
		this.eventProcessor.running = false;
		try {
			this.eventProcessor.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (eventProcessor.cycleDetector != null) {
			Vector<Deadlock> deadlocks = eventProcessor.cycleDetector.findDeadlocks();
			if (deadlocks.size() > 0)
				System.out.println("found " + deadlocks.size() + " deadlocks. retrieving signatures...");
			for (Deadlock dlck : deadlocks) {
				eventProcessor.cycleDetector.saveToHistory(dlck);
			}
		}
		
		//dump history again, to update the matching depths on disk
		try {
			PrintWriter pw = new PrintWriter(this.histFile);
			for (Signature sig: this.history.historyQueue) {
				pw.write(sig+ System.getProperty("line.separator"));				
			}
			pw.close();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}		

		int nfps = 0;
		int ntps = 0;
		for (Signature sig: history.historyQueue) {
			nfps += sig.nFP.get();
			ntps += sig.nTP.get();
		}
		
		double duration = System.nanoTime()- this.startTime;
		double syncThroughput = this.nSyncs.get()/ (duration/ 1000/ 1000/ 1000);
		
//		System.out.println(syncThroughput+ " syncs/sec, "+ this.nYields.get()+ " yields, "+ nfps+ " FPs, "+ ntps+ " TPs, "+ this.eventProcessor.cycleDetector.foundCycles.size()+ " yield cycles");
	}
	
	public Vector<Pair<Integer, Integer>> getIdsSignaturesToSkip(String className, String methodName, int lineNumber, boolean before) {
		Vector<Pair<Integer, Integer>> sigIds = new Vector<Pair<Integer, Integer>>();
		for (SkipAvoidance skipAv: SkipAvoidanceAnalysis.instance.positionsToSkipAvoidance) {
			if (skipAv.before == before && skipAv.position.getLineNumber() == lineNumber && skipAv.position.getClassName().equals(className) && skipAv.position.getMethodName().equals(methodName)) {
				sigIds.add(new Pair<Integer, Integer>(skipAv.sigId, skipAv.index));
			}
		}
		return sigIds;
	}
	
	public static void match(int sigId, int index, int depth) {
		int tid = (int)Thread.currentThread().getId();
		Signature sig = DimmunixDeadlock.instance.history.historyMap[sigId];
		SignaturePosition p = sig.positions.get(index);
		int pDepth = p.depth; 		
		
		if (depth > pDepth) {
			return;
		}
		
		if (depth == pDepth) {
			p.matches[tid] = depth- 1;
		}
		else if (depth == p.matches[tid]) {
			p.matches[tid]--;
		}		
	}
}
