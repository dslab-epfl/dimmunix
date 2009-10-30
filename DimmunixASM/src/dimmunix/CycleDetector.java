package dimmunix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;

public class CycleDetector {
	Dimmunix dImmunix;
	
	Vector<Cycle> preallocatedCycles = new Vector<Cycle>(100);	
	Vector<Node> traversedNodes = new Vector<Node>(1000);
	HashSet<Node> joinNodes = new HashSet<Node>(256);
	Cycle bufferCycle = new Cycle(1000);
	Vector<Cycle> cycles = new Vector<Cycle>(100);	
	
	public CycleDetector(Dimmunix dImmunix) {
		this.dImmunix = dImmunix;
		for (int i = 0; i < preallocatedCycles.capacity(); i++)
			preallocatedCycles.add(new Cycle());
	}
	
	void processEvent(Event evt) {
		if (evt.type == EventType.REQUEST) {
			dImmunix.rag.request(evt.thread, evt.lock, evt.position);
		}
		else if (evt.type == EventType.ACQUIRE) {
			dImmunix.rag.lock(evt.thread, evt.lock, evt.position);
		}
		else if (evt.type == EventType.RELEASE) {
			dImmunix.rag.unlock(evt.thread, evt.lock);
		}
		else if (evt.type == EventType.YIELD) {
			YieldEvent yevt =(YieldEvent)evt; 
			yevt.thread.threadYields.clear();
			yevt.thread.posYields.clear();
			yevt.thread.yieldCauseTemplate = yevt.matchingTemplate;
			for (int i = 0; i < yevt.yields.size(); i++) {
				if (evt.thread != yevt.yields.get(i).thread) {
					evt.thread.threadYields.add(yevt.yields.get(i).thread);					
					evt.thread.posYields.add(yevt.yields.get(i).position);
				}
			}
		}
		else if (evt.type == EventType.GRANT) {
			evt.thread.threadYields.clear();
			evt.thread.posYields.clear();
			evt.thread.yieldCauseTemplate = null;
		}		
	}
	
	Cycle getNewCycle(Cycle buffer) {
		if (preallocatedCycles.isEmpty()) {
			for (int i = 0; i < preallocatedCycles.capacity()/ 10; i++)
				preallocatedCycles.add(new Cycle());			
		}
		Cycle c = preallocatedCycles.remove();
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
			if (x.next == start)
				addCycle(bufferCycle);
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
		for (int i = 0; i < cycles.size(); i++)
			preallocatedCycles.add(cycles.get(i));
		cycles.clear();
		
		if (hasCycles(x)) {
			for (Node jn: joinNodes)
				getCyclesRec(jn, jn);
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
	
	void bypassAvoidanceIfLivelocked() {
		for (int i = 0; i < cycles.size(); i++) {
			Cycle c = cycles.get(i); 
			if (!c.isDeadlock()) {
				for (int j = 0; j < c.size(); j++) {
					if (c.isYieldEdge(j)) {
						((ThreadNode)c.nodes.get(j)).resumeFromLivelock();
						break;
					}
				}
			}
		}		
	}
	
	Signature template(Cycle cycle) {
		boolean isDeadlock = cycle.isDeadlock();
		Signature tmpl = new Signature(isDeadlock);
		
		for (int i = 0; i < cycle.size(); i++) {
			if (cycle.isYieldEdge(i) || cycle.isHoldEdge(i)) {
				tmpl.add(new SignaturePosition(cycle.positions.get(i), isDeadlock? 1: cycle.positions.get(i).size(), dImmunix));
//				tmpl.addInner(this.getInnerPosition((ThreadNode)cycle.nodes.get((i+ 1)% cycle.size())));
			}
		}
		return tmpl;
	}
	
	void saveToHistory(Signature tmpl) {
		if (dImmunix.history.contains(tmpl))
			return;
		
		dImmunix.history.add(tmpl);		
		for (SignaturePosition tpos: tmpl.positions)
			tpos.refreshMatchingPositionsInHist();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(dImmunix.histFile, true));
			bw.write(tmpl+ System.getProperty("line.separator"));
			bw.close();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}		
	}	
	
	void checkForCycles() {
		for (ThreadNode t: dImmunix.rag.requestingThreads) {
			if (this.getNewCycles(t)) {
				this.filterYieldCycles();
				this.bypassAvoidanceIfLivelocked();					
				for (int i = 0; i < cycles.size(); i++)
					this.saveToHistory(this.template(cycles.get(i)));
			}
		}
	}
	
	InnerPosition getInnerPosition(ThreadNode t) {
		StackTraceElement[] trace = t.thread.getStackTrace();
		Vector<String> callStack = new Vector<String>(10);
		
		for (int i = 0, depth = 0; i < trace.length && depth < dImmunix.maxCallStackDepth; i++, depth++)
			callStack.add(trace[i].toString());
		return new InnerPosition(callStack);
	}
}
