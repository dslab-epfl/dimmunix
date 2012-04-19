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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashSet;

import org.aspectj.weaver.loadtime.SyncPositions;

import communix.Client;

import dimmunix.Configuration;
import dimmunix.Util;
import dimmunix.Vector;

public class CycleDetector {
	DimmunixDeadlock dimmunix;
	
	Vector<Node> traversedNodes = new Vector<Node>(1000);
	HashSet<Node> joinNodes = new HashSet<Node>(256);
	Cycle bufferCycle = new Cycle(1000);
	Vector<Cycle> cycles = new Vector<Cycle>(100);
	
	public volatile int nYieldCycles = 0;
	HashSet<Cycle> foundCycles = new HashSet<Cycle>();
	
	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	static final int explicitLockOffset = 7;
	
	public CycleDetector(DimmunixDeadlock dImmunix) {
		this.dimmunix = dImmunix;
	}
	
	void processEvent(Event evt) {
		if (evt.type == EventType.REQUEST) {
			dimmunix.rag.request(evt.thread, evt.lock, evt.position);
		}
		else if (evt.type == EventType.ACQUIRE) {
			dimmunix.rag.lock(evt.thread, evt.lock, evt.position);
		}
		else if (evt.type == EventType.RELEASE) {
			dimmunix.rag.unlock(evt.thread, evt.lock);
		}
		else if (evt.type == EventType.YIELD) {
			YieldEvent yevt =(YieldEvent)evt; 
			yevt.thread.threadYields.clear();
			yevt.thread.lockYields.clear();
			yevt.thread.posYields.clear();
			yevt.thread.yieldCauseTemplate = yevt.matchingTemplate;
			for (int i = 0; i < yevt.yields.size(); i++) {
				if (evt.thread != yevt.yields.get(i).thread) {
					evt.thread.threadYields.add(yevt.yields.get(i).thread);
					evt.thread.lockYields.add(yevt.yields.get(i).lock);
					evt.thread.posYields.add(yevt.yields.get(i).position);
				}
			}
			dimmunix.rag.yieldingThreads.add(evt.thread);
		}
		else if (evt.type == EventType.YIELD_SIMPLE) {
			dimmunix.rag.yieldingThreads.add(evt.thread);			
		}
		else if (evt.type == EventType.GRANT) {
			evt.thread.threadYields.clear();
			evt.thread.lockYields.clear();
			evt.thread.posYields.clear();
			evt.thread.yieldCauseTemplate = null;
			dimmunix.rag.yieldingThreads.remove(evt.thread);
		}		
		else if (evt.type == EventType.GRANT_SIMPLE) {
			dimmunix.rag.yieldingThreads.remove(evt.thread);			
		}
	}
	
	Cycle getNewCycle(Cycle buffer) {
		Cycle c = new Cycle();
		c.nodes.clear();
		c.positions.clear();
		for (int i = 0; i < buffer.size(); i++) {
			c.nodes.add(buffer.nodes.get(i));
			c.positions.add(buffer.positions.get(i));
		}
		return c;
	}
	
	boolean hasCycles(Node x) {
		x.color = Color.GREY;
		traversedNodes.add(x);
		
		if (x.next == null) {
			x.color = Color.BLACK;
			return false;
		}
		
		boolean bcycle = false;
		
		if (x.next.color == Color.GREY) {
			joinNodes.add(x.next);
			bcycle = true;
		}
		else if (x.next.color == Color.WHITE) {
			bcycle = hasCycles(x.next);
		}
		
		if (x instanceof LockNode) {
			if (!bcycle)
				x.color = Color.BLACK;
			return bcycle;
		}
		
		ThreadNode t = (ThreadNode)x;
		
		if (t.threadYields.isEmpty()) {
			if (!bcycle)
				x.color = Color.BLACK;
			return bcycle;			
		}
		
		boolean byieldCycles = true;
		
		for (int i = 0; i < t.threadYields.size(); i++) {
			boolean b = false;
			if (t.threadYields.get(i).color == Color.GREY) {
				joinNodes.add(t.threadYields.get(i));
				b = true;
			}
			else if (t.threadYields.get(i).color == Color.WHITE) {
				b = hasCycles(t.threadYields.get(i));
			}
			if (b == false) {
				byieldCycles = false;
				break;
			}
		}
		
		if (!bcycle && !byieldCycles)
			x.color = Color.BLACK;
		return bcycle || byieldCycles;
	}
	
	void addCycle(Cycle bufferCycle) {
		cycles.add(getNewCycle(bufferCycle));
		if (!bufferCycle.isDeadlock()) {
			for (int i = 0; i < bufferCycle.size(); i++) {
				if (bufferCycle.isYieldEdge(i))
					((ThreadNode)bufferCycle.nodes.get(i)).yieldCauseTemplate.nYieldCycles++;
			}				
		}
	}
	
	void getCyclesRec(Node start, Node x) {
		if (x instanceof LockNode) {
			bufferCycle.add(x, x.posNext);
			if (x.next == start) {
				addCycle(bufferCycle);
			}
			else {
				if (!bufferCycle.contains(x.next))
					getCyclesRec(start, x.next);
			}
			bufferCycle.remove();
		}
		else {
			if (x.next.color == Color.GREY) {
				bufferCycle.add(x, x.posNext);
				getCyclesRec(start, x.next);
				bufferCycle.remove();
			}
			ThreadNode t = (ThreadNode)x;
			if (t.threadYields.size() > 0 && t.allYieldsGrey()) {
				for (int i = 0; i < t.threadYields.size(); i++) {
					bufferCycle.add(x, t.posYields.get(i));
					if (t.threadYields.get(i) == start)
						addCycle(bufferCycle);
					else {
						if (!bufferCycle.contains(t.threadYields.get(i)))
							getCyclesRec(start, t.threadYields.get(i));
					}
					bufferCycle.remove();
				}
			}
		}
	}
	
	boolean getNewCycles(Node x) {
		for (int i = 0; i < traversedNodes.size(); i++)
			traversedNodes.get(i).color = Color.WHITE;
		traversedNodes.clear();
		joinNodes.clear();
		cycles.clear();
		
		if (hasCycles(x)) {
			for (Node jn: joinNodes) {
				getCyclesRec(jn, jn);
			}
			return !cycles.isEmpty();
		}
		return false;
	}
	
	void filterYieldCycles() {
		//keep all deadlocks and choose the livelock with max size
		int maxSize = 0;
		for (int i = 0; i < cycles.size(); i++) {
			if (!cycles.get(i).isDeadlock() && cycles.get(i).size() > maxSize)
				maxSize = cycles.get(i).size();
		}
		boolean foundMaxLivelock = false;
		for (int i = 0; i < cycles.size(); i++) {			
			if (!cycles.get(i).isDeadlock()) {
				if (foundMaxLivelock == false && cycles.get(i).size() == maxSize)
					foundMaxLivelock = true;
				else 
					cycles.remove(i--);
			}
		}
	}
	
	void bypassLivelock() {
		if (dimmunix.simpleAvoidance) {
			for (Cycle c: cycles) {
				for (int i = 0; i < c.size(); i++) {
					if (c.isRequestEdge(i)) {
						ThreadNode t = (ThreadNode)c.nodes.get(i);
						if (!dimmunix.rag.yieldingThreads.contains(t))
							continue;
						for (Signature sig: new Vector<Signature>(t.currentTemplates)) {
							sig.lock.lockNode.semNext = null;//release event
							sig.lock.release();
						}					
					}
				}
			}
//			for (Signature sig: dimmunix.history.historyQueue) {
//				sig.lock.lockNode.semNext = null;//release event
//				sig.lock.release();				
//			}
		}
		else {
			for (Cycle c: cycles) {
				for (int i = 0; i < c.size(); i++) {
					if (c.isYieldEdge(i)) {
						ThreadNode t = (ThreadNode)c.nodes.get(i); 
						t.bypassLivelock();
					}
				}
			}
		}
	}
	
	Deadlock getDeadlockInfo(Cycle cycle) {
		Deadlock dlck = new Deadlock(true);
		for (int i = 0; i < cycle.size(); i++) {
			if (cycle.isRequestEdge(i)) {
				ThreadNode t = (ThreadNode)cycle.nodes.get(i);
				LockNode l = (LockNode)cycle.nodes.get((i+ 1)% cycle.size());
				
				long[] threadIds = {t.thread.getId()};
				ThreadInfo tinfo = threadMXBean.getThreadInfo(threadIds, true, true)[0]; 
				dlck.threads.add(tinfo);
				dlck.lockNodes.add(l);
				dlck.locks.add(null);
				dlck.callStacks.add(t.thread.getStackTrace());
				dlck.lockStacks.add(tinfo.getLockedMonitors());
				dlck.ownableSyncStacks.add(tinfo.getLockedSynchronizers());				
			}
			else if (cycle.isYieldEdge(i)) {
				ThreadNode t1 = (ThreadNode)cycle.nodes.get(i);
				ThreadNode t2 = (ThreadNode)cycle.nodes.get((i+ 1)% cycle.size());
				
				LockNode l = null;
				for (int j = 0; j < t1.threadYields.size(); j++) {
					if (t1.threadYields.get(j) == t2) {
						l = t1.lockYields.get(j);
						break;
					}
				}				
				
				long[] threadIds = {t1.thread.getId()};
				ThreadInfo tinfo = threadMXBean.getThreadInfo(threadIds, true, true)[0]; 
				dlck.threads.add(tinfo);
				dlck.lockNodes.add(l);
				dlck.locks.add(null);
				StackTraceElement[] callStack = t1.thread.getStackTrace();
				dlck.callStacks.add(Arrays.copyOfRange(callStack, dimmunix.yieldCallStackOffset, callStack.length));
				dlck.lockStacks.add(tinfo.getLockedMonitors());
				dlck.ownableSyncStacks.add(tinfo.getLockedSynchronizers());								
			}
		}
		
		return dlck;
	}
		
	void saveToHistory(Deadlock dlck) {
		Signature sig;
		try {
			 sig = dlck.getSignature();
		}
		catch (Throwable ex) {
			ex.printStackTrace();
			return;
		}
		
		if (dimmunix.history.historyQueue.contains(sig))
			return;
		
		if (dimmunix.history.merge(sig)) {
			//if we can merge it with one of the existing sigs, we don't add it
			return;
		}
		
		dimmunix.newCyclesFound = true;
		
		dimmunix.history.add(sig);	
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(dimmunix.histFile, true));
			bw.write(sig+ System.getProperty("line.separator"));
			bw.close();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}		
		
		//save positions to instrument
		for (StackTraceElement syncPos: dlck.getSyncPositions(sig)) {
			SyncPositions.add(syncPos);
		}
		
		//send signature to communix server
		if (Configuration.instance.communixEnabled) {
			Client.instance.sendSignature(sig);			
		}
	}	
	
	public void checkForYieldCycles() {
//		System.out.println("checking for yield cycles");
		this.dimmunix.rag.update();
		
		for (ThreadNode t: dimmunix.rag.yieldingThreads) {
			if (this.getNewCycles(t)) {
				
				int nFoundCycles = foundCycles.size();  
				foundCycles.addAll(cycles);
				int nNewCycles = foundCycles.size()- nFoundCycles;
				
				if (nNewCycles > 0) {
					if (dimmunix.simpleAvoidance) {
						System.out.println("found "+ cycles.size()+ " yield cycles. retrieving signatures...");					
					}
					else {
						System.out.println("found "+ cycles.size()+ " yield cycles caused by sig "+ t.curInstance.template.id+ ". retrieving signatures...");										
					}
					
					this.filterYieldCycles();
					
					for (int i = 0; i < cycles.size(); i++) {
						try {
							this.saveToHistory(this.getDeadlockInfo(cycles.get(i)));
						}
						catch (Throwable ex) {
							ex.printStackTrace();
						}
					}					
				}
				
				this.bypassLivelock();								
			}
		}		
	}
	
	InnerPosition getInnerPosition(ThreadNode t) {
		StackTraceElement[] trace = t.thread.getStackTrace();
		Vector<StackTraceElement> callStack = new Vector<StackTraceElement>(dimmunix.maxCallStackDepth);
		
		for (int i = 0, depth = 0; i < trace.length && depth < dimmunix.maxCallStackDepth; i++, depth++)
			callStack.add(trace[i]);
		return new InnerPosition(callStack);
	}
	
	public Vector<Deadlock> findDeadlocks() {
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		
		long[] deadlockedThreads = threadBean.findDeadlockedThreads();
		
		Vector<Deadlock> deadlocks = new Vector<Deadlock>();
		
		if (deadlockedThreads != null) {
			ThreadInfo[] threadsInfo = threadBean.getThreadInfo(deadlockedThreads, true, true);
			
			Deadlock deadlock = new Deadlock(false);
			deadlocks.add(deadlock);
			for (int i = 0; i < threadsInfo.length; i++) {
				ThreadInfo tinfo = threadsInfo[i];
				
//				System.out.println("thread #"+ i);
//				System.out.println("id = "+ tinfo.getThreadId());
//				System.out.println("waiting for "+ tinfo.getLockInfo()+ ", held by "+ tinfo.getLockOwnerId());
//				System.out.println("lockStack = "+ Arrays.toString(tinfo.getLockedMonitors()));
//				System.out.println("ownableStack = "+ Arrays.toString(tinfo.getLockedSynchronizers()));
				
				deadlock.threads.add(tinfo);
				deadlock.locks.add(tinfo.getLockInfo());
				deadlock.lockNodes.add(null);
				StackTraceElement[] callStack = Util.getThread(tinfo.getThreadId()).getStackTrace();
				if (callStack[0].getMethodName().equals("park") && callStack[0].getClassName().equals("sun.misc.Unsafe")) {
					//it's an explicit lock
					callStack = Arrays.copyOfRange(callStack, explicitLockOffset, callStack.length);
//					System.out.println(Arrays.toString(callStack));
				}
				deadlock.callStacks.add(callStack);
				deadlock.lockStacks.add(tinfo.getLockedMonitors());
				deadlock.ownableSyncStacks.add(tinfo.getLockedSynchronizers());
				
				if (i < threadsInfo.length- 1 && tinfo.getLockOwnerId() != threadsInfo[i+ 1].getThreadId()) {
					deadlock = new Deadlock(false);
					deadlocks.add(deadlock);
				}				
			}			
		}
		
		return deadlocks;
	}
}
