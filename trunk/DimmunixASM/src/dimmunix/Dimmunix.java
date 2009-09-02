package dimmunix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Dimmunix {
	
	public static Dimmunix dimmunix;
	
	public static final int NOTHING = 0;
	public static final int EVENTS0 = 1;
	public static final int LOOKUP = 2;
	public static final int EVENTS = 3;
	public static final int UPDATES = 4;
	public static final int AVOIDANCE = 5;
	public static final int FULL = 6;
	
	int experimentLevel = FULL; 
	int maxCallStackDepth = 10;
	boolean ignoreLocksInAvoidance = false;
	int cycleDetectionPeriodMsec = 100;//msec
	boolean adjustPrecision = true;//automatically adjusting call paths' depths	
	boolean collaborativeYield = true;
	boolean saveLogPeriodically = false;
	boolean ignoreAvoidance = false;
	int yieldTimeout = 0;
	
	final String appName = "Dimmunix";
	
	ResourceAllocationGraph rag;	
	ConcurrentLinkedQueue<Signature> history = new ConcurrentLinkedQueue<Signature>();
	ConcurrentLinkedQueue<Thread> appThreads = new ConcurrentLinkedQueue<Thread>();
	
	final String workDir = System.getProperty("user.home")+ 
								System.getProperty("file.separator")+ 
								appName+
								System.getProperty("file.separator");
	final String histFile = workDir+ appName+ ".hist";
	final String logFile = workDir+ appName+ ".log";
	final String confFile = workDir+ appName+ ".conf";
	
	public EventProcessor eventProcessor;
	
	int startCallStackIndex = 5;
	
	long startTime;
	
	public Dimmunix() {
		dimmunix = this;		
		
		if (System.getProperty("java.runtime.version").substring(0, 3).equals("1.6"))
			startCallStackIndex = 4;
		
		this.loadConfiguration();
		
		this.rag = new ResourceAllocationGraph(this);
		
		this.loadHistory();

		this.eventProcessor = new EventProcessor(this);
		this.eventProcessor.start();	
		
		startTime = System.nanoTime();
	}
	
	public void parseExperimentLevel(String val) {
		if (val.equals("NOTHING"))
			experimentLevel = NOTHING;
		else if (val.equals("LOOKUP"))
			experimentLevel = LOOKUP;
		else if (val.equals("EVENTS0"))
			experimentLevel = EVENTS0;
		else if (val.equals("EVENTS"))
			experimentLevel = EVENTS;
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
		case EVENTS:
			return "EVENTS";
		case EVENTS0:
			return "EVENTS0";
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
				else if (key.equals("saveLogPeriodically"))
					saveLogPeriodically = Boolean.parseBoolean(val);
				else if (key.equals("ignoreAvoidance"))
					ignoreAvoidance = Boolean.parseBoolean(val);
			}
			br.close();
		} 
		catch (Exception e) {
			try {
				new File(workDir).mkdir();
				BufferedWriter bw = new BufferedWriter(new FileWriter(confFile));
				bw.write("experimentLevel = "+ toStringExperimentLevel()+ System.getProperty("line.separator"));
				bw.write("maxCallStackDepth = "+ maxCallStackDepth+ System.getProperty("line.separator"));
				bw.write("ignoreLocksInAvoidance = "+ ignoreLocksInAvoidance+ System.getProperty("line.separator"));
				bw.write("collaborativeYield = "+ collaborativeYield+ System.getProperty("line.separator"));
				bw.write("cycleDetectionPeriodMsec = "+ cycleDetectionPeriodMsec+ System.getProperty("line.separator"));
				bw.write("adjustPrecision = "+ adjustPrecision+ System.getProperty("line.separator"));
				bw.write("saveLogPeriodically = "+ saveLogPeriodically+ System.getProperty("line.separator"));
				bw.write("ignoreAvoidance = "+ ignoreAvoidance+ System.getProperty("line.separator"));
				bw.close();
			}
			catch (IOException ex) {
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
				Signature templ = new Signature(lineTok.nextToken().equals("deadlock_template"));
				StringTokenizer templTok = new StringTokenizer(lineTok.nextToken(), ";");
				
				while (templTok.hasMoreTokens()) {
					StringTokenizer posTok = new StringTokenizer(templTok.nextToken(), "#");
					int depth = Integer.parseInt(posTok.nextToken()); 
					
					StringTokenizer stackTok = new StringTokenizer(posTok.nextToken(), ",");
					CallStack callStack = new CallStack(maxCallStackDepth);
					int d = 0;
					while (stackTok.hasMoreTokens() && d++ < maxCallStackDepth)
						callStack.add(stackTok.nextToken());
					templ.add(new SignaturePosition(rag.getPosition(callStack), depth, this));

					try {
						StringTokenizer innerStackTok = new StringTokenizer(posTok.nextToken(), ",");
						Vector<String> innerCallStack = new Vector<String>(maxCallStackDepth);
						d = 0;
						while (innerStackTok.hasMoreTokens() && d++ < maxCallStackDepth)
							innerCallStack.add(innerStackTok.nextToken());
						templ.addInner(new InnerPosition(innerCallStack));						
					}
					catch (NoSuchElementException e) {						
					}
				}
				history.add(templ);
			}
			br.close();
			
			for (Signature tmpl: history) {
				for (SignaturePosition tpos: tmpl.positions)
					tpos.refreshMatchingPositionsInHist();
			}
		} 
		catch (FileNotFoundException e) {
			try {
				new File(histFile).createNewFile();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		catch (Exception e) {		
			e.printStackTrace();
		}
	}
	
	boolean instantiatesTemplates(ThreadNode t, Position p) {
		for (int i = 0; i < t.currentTemplates.size(); i++) {
			if (this.instance(t, t.currentTemplates.get(i), t.currentIndicesInTemplates.get(i), p))
				return true;
		}
		return false;
	}
	
	boolean instance(ThreadNode t, Signature templ, int index, Position p) {
		if (templ.size() > t.currentMatchingPositions.size()) {
			int n = templ.size()- t.currentMatchingPositions.size();
			for (int i = 0; i < n; i++)
				t.currentMatchingPositions.add(new Vector<Position>(10));
		}
		
		for (int i = 0; i < templ.size(); i++) {
			if (i != index)
				templ.positions.get(i).getMatchingPositions(t.currentMatchingPositions.get(i));
		}
		
		return instanceRec(0, t, templ, index, p);
	}
	
	boolean instanceRec(int n, ThreadNode t, Signature templ, int index, Position p) {
		boolean result = false;
		if (n == templ.size()) {
			/*if (templ.positions.get(0).value != t.currentPositionsToMatch.get(0))
				System.out.println((templ.positions.get(0).value == t.currentPositionsToMatch.get(1))+ " "+ (templ.positions.get(1).value == t.currentPositionsToMatch.get(0)));
			else
				System.out.println((templ.positions.get(0).value == t.currentPositionsToMatch.get(0))+ " "+ (templ.positions.get(1).value == t.currentPositionsToMatch.get(1)));				
			System.out.println(index+ " "+ t.currentPositionsToMatch.get(0).lockGrants+ " "+ t.currentPositionsToMatch.get(1).lockGrants);
			*/
			result = instance(templ, index, t);
		}
		else if (n == index) {
			t.currentPositionsToMatch.add(p);
			result = instanceRec(n+ 1, t, templ, index, p);
			t.currentPositionsToMatch.remove();
		}
		else {
			Vector<Position> positions = t.currentMatchingPositions.get(n); 
			for (int i = 0; i < positions.size(); i++) {
				t.currentPositionsToMatch.add(positions.get(i));
				result = instanceRec(n+ 1, t, templ, index, p);
				t.currentPositionsToMatch.remove();
				if (result)
					break;
			}
		}
		return result;
	}
	
	boolean instance(Signature templ, int index, ThreadNode t) {
		t.currentLockGrantsIterators.setSize(templ.size());
		t.templateInstance.setSize(templ.size());
		t.templateInstance.template = templ;
		
		t.templateInstance.set(index, t.currentLockGrant);
		
		for (int i = 0; i < templ.size(); i++) {
			if (i != index && t.currentPositionsToMatch.get(i).lockGrants.isEmpty()) {
				return false;
			}
		}
		
		for (int i = 0; i < templ.size(); i++) {
			if (i != index)
				t.currentLockGrantsIterators.set(i, t.currentPositionsToMatch.get(i).lockGrants.iterator());
		}

		int k = 0;
		while (k >= 0) {
			if (k == templ.size())
				return true;
			if (k == index) {
				if (t.templateInstance.isDisjointUntil(k, t.currentLockGrant))
					k++;
				else
					k--;
				continue;
			}

			Iterator<LockGrant> lockGrants = t.currentLockGrantsIterators.get(k);

			boolean found = false;
			while (lockGrants.hasNext()) {
				LockGrant lg = lockGrants.next();
				if (t.templateInstance.isDisjointUntil(k, lg)) {
					t.templateInstance.set(k, lg);
					found = true;
					break;
				}
			}

			if (found)
				k++;
			else {
				t.currentLockGrantsIterators.set(k, t.currentPositionsToMatch.get(k).lockGrants.iterator());
				k--;
				if (k == index)
					k--;
			}
		}

		return false;
	}
	
	void avoidance(Thread t, Object l) {
		if (l == null || experimentLevel == Dimmunix.NOTHING)
			return;
		
		ThreadNode tnode = rag.getThreadNode(Thread.currentThread());
		LockNode lnode = rag.getLockNode(l);
		
		if (lnode.owner == tnode)
			return;
		
		if (experimentLevel == Dimmunix.EVENTS0) {
			tnode.nSyncs++;
			return;
		}
		
		getCallStack(tnode);
		Position p = rag.getPosition(tnode.currentCallStack);
		getTemplatesContainingCurrentPosition(tnode, p);
		
		if (experimentLevel == Dimmunix.LOOKUP)
			return;		

		tnode.reqPos = p;
		
		if (!tnode.reqPosInHistory) {
			eventProcessor.addRequestEvent(tnode, lnode, p, tnode.reqPosInHistory);
			eventProcessor.addGrantEvent(tnode, lnode, p, tnode.reqPosInHistory);
			return;
		}		
		eventProcessor.addRequestEvent(tnode, lnode, p, tnode.reqPosInHistory);
		
		while (!request(tnode, lnode, p)) {
			if (collaborativeYield) {
				for (int i = 0; i < tnode.templateInstance.size(); i++) {
					if (tnode.templateInstance.lockGrants.get(i).thread != tnode)					
						tnode.templateInstance.lockGrants.get(i).yieldersLock.readLock().lock();
				}
				if (tnode.templateInstance.isActive(tnode)) {
					synchronized (tnode.yieldLock) {
						for (int i = 0; i < tnode.templateInstance.size(); i++) {
							if (tnode.templateInstance.lockGrants.get(i).thread != tnode) {
								tnode.templateInstance.lockGrants.get(i).yielders.add(tnode);
								tnode.templateInstance.lockGrants.get(i).yieldersLock.readLock().unlock();					
							}
						}
						
						tnode.resetYieldCauseTo(tnode.templateInstance);	
						eventProcessor.addYieldEvent(tnode, lnode, p, tnode.yieldCause);						
						
						if (!ignoreAvoidance) {
							try {
								tnode.yieldLock.wait(yieldTimeout);
								if (!tnode.isNotified()) {
									tnode.bypassAvoidance = true;
									//System.exit(0);
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}							
						}
						else {
							tnode.bypassAvoidance = true;
						}
					}	
					
					//eventProcessor.addWakeUpEvent(tnode, lnode, p);

					if (tnode.bypassAvoidance) {
						this.grant(tnode, ignoreLocksInAvoidance? null: lnode, p);
						eventProcessor.addGrantEvent(tnode, lnode, p, tnode.reqPosInHistory);

						tnode.bypassAvoidance = false;							
						return;
					}							
				}
				else {
					for (int i = 0; i < tnode.templateInstance.size(); i++) {
						if (tnode.templateInstance.lockGrants.get(i).thread != tnode)					
							tnode.templateInstance.lockGrants.get(i).yieldersLock.readLock().unlock();
					}
				}				
			}
			else {
				eventProcessor.addYieldEvent(tnode, lnode, p, tnode.templateInstance);
				if (!ignoreAvoidance) {
					Thread.yield();
					//eventProcessor.addWakeUpEvent(tnode, lnode, p);
				}
				else {
					this.grant(tnode, ignoreLocksInAvoidance? null: lnode, p);
					eventProcessor.addGrantEvent(tnode, lnode, p, tnode.reqPosInHistory);
					return;
				}
			}
		}
		
		if (experimentLevel >= EVENTS)
			this.eventProcessor.addGrantEvent(tnode, lnode, p, tnode.reqPosInHistory);
	}
	
	void grant(ThreadNode t, LockNode l, Position p) {
		t.currentLockGrant = p.grant(t, l);
	}
	
	boolean request(ThreadNode t, LockNode l, Position p) {
		LockNode lAv = (ignoreLocksInAvoidance)? null: l;
		if (experimentLevel >= AVOIDANCE) {
			this.grant(t, lAv, p);
			boolean unsafe = this.instantiatesTemplates(t, p); 
			if (unsafe) {
				this.ungrant(t.currentLockGrant);
				return false;				
			}
			else {	
				return true;
			}
		}
		else {
			if (experimentLevel >= UPDATES)
				this.grant(t, lAv, p);
			if (experimentLevel >= EVENTS)
				this.eventProcessor.addGrantEvent(t, l, p, t.reqPosInHistory);
			return true;
		}
	}
	
	void acquire(Thread t, Object l) {
		if (l == null || dimmunix.experimentLevel == Dimmunix.NOTHING)
			return;		
		
		ThreadNode tnode = this.rag.getThreadNode(t);
		LockNode lnode = this.rag.getLockNode(l);
		
		if (experimentLevel == LOOKUP)
			return;
		
		if (lnode.owner != tnode) {
			lnode.owner = tnode;
			if (experimentLevel >= EVENTS) {
				lnode.acqPos = tnode.reqPos;
				lnode.acqPosInHistory = tnode.reqPosInHistory;
				this.eventProcessor.addAcquireEvent(tnode, lnode, lnode.acqPos, lnode.acqPosInHistory);
			}
		}
		lnode.nlck++;		
	}
	
	void release(Thread t, Object l) {
		if (l == null || dimmunix.experimentLevel == Dimmunix.NOTHING)
			return;		
				
		ThreadNode tnode = this.rag.getThreadNode(t);
		LockNode lnode = this.rag.getLockNode(l);
		
		if (experimentLevel == LOOKUP)
			return;
		
		if (lnode.owner != tnode)
			return;

		if (--lnode.nlck == 0) {
			lnode.owner = null;
			Position p = lnode.acqPos;
			lnode.acqPos = null;

			if (experimentLevel < EVENTS)
				return;

			if (!lnode.acqPosInHistory) {
				this.eventProcessor.addReleaseEvent(tnode, lnode, null, lnode.acqPosInHistory);										
			}
			else {
				if (experimentLevel >= UPDATES) {
					LockGrant lg = (ignoreLocksInAvoidance)? p.findLockGrant(tnode, null): p.findLockGrant(tnode, lnode);
					this.ungrant(lg);
				}
			}				
		}			
	}
	
	int ungrant(LockGrant lg) {
		if (lg.n == 1 && collaborativeYield && experimentLevel >= AVOIDANCE)
			lg.yieldersLock.writeLock().lock();
		if (lg.n == 1)
			this.eventProcessor.addReleaseEvent(lg.thread, lg.lock, lg.position, lg.lock.acqPosInHistory);						
		int n = lg.position.ungrant(lg); 
		if (n == 0 && collaborativeYield && experimentLevel >= AVOIDANCE) {
			this.wakeUpYieldingThreads(lg);
			lg.yieldersLock.writeLock().unlock();
		}	
		return n;
	}

	void wakeUpYieldingThreads(LockGrant lg) {
		if (!ignoreAvoidance) {
			for (ThreadNode yt: lg.yielders) {
				synchronized (yt.yieldLock) {				
					if (!yt.yieldCause.isEmpty() && yt.yieldCause.contains(lg)) {
						yt.yieldCause.clear();
						yt.yieldLock.notify();
					}
				}
			}			
		}
		lg.yielders.clear();
	}
	
	void getCallStack(ThreadNode t) {
		StackTraceElement[] trace = t.thread.getStackTrace();
		
		t.currentCallStack.clear();	
		for (int i = startCallStackIndex, depth = 0; i < trace.length && depth < maxCallStackDepth; i++, depth++)
			t.currentCallStack.add(trace[i]);
	}
	
	void getTemplatesContainingCurrentPosition(ThreadNode t, Position p) {
		t.reqPosInHistory = false;
		t.currentTemplates.clear();
		t.currentIndicesInTemplates.clear();
		for (Signature tmpl: history) {
			for (int i = 0; i < tmpl.positions.size(); i++) {
				/*Position posHist = tmpl.positions.get(i).value;
				if (posHist == p) {
					
				}*/
				if (tmpl.positions.get(i).match(p)) {
					
					t.currentTemplates.add(tmpl);
					t.currentIndicesInTemplates.add(i);
					t.reqPosInHistory = true;
					break;
				}
			}
		}
	}
	
	void refreshMatchingPositions(Position pNew) {
		for (Signature tmpl: dimmunix.history) {
			for (int i = 0; i < tmpl.size(); i++) {
				tmpl.positions.get(i).matchAndAdd(pNew);
			}
		}		
	}
	
	void refreshHistory() {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(dimmunix.histFile, false));
			for (Signature tmpl: dimmunix.history)
				bw.write(tmpl+ System.getProperty("line.separator"));
			bw.close();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
