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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashSet;

public class Logger {
	Dimmunix dImmunix;
	
	static int nAppThreads = 0;
	static int nSyncThreads = 0;
	static int nLocks = 0;
	static int nPositions = 0;
	static long nPositionsHist = 0;
	Vector<Position> positionsInHist = new Vector<Position>(1000);
	public static int nYields = 0;
//	static long tYields = 0;
	static long nSyncs = 0;
	static long nSyncsHist = 0;
//	static long tAvoidances = 0;
	public static long peakMemConsumption = 0;
	public static float avgDepth = 0;
	
	HashSet<ThreadNode> threads = new HashSet<ThreadNode>(2048);
	//Vector<Event> currentRequests = new Vector<Event>(10000);
	//Vector<Event> currentYields = new Vector<Event>(10000);
	
	public Logger(Dimmunix dImmunix) {
		this.dImmunix = dImmunix;
	}
	
	void processEvent(Event evt) {
		if (evt.type == EventType.REQUEST) {
			if (threads.add(evt.thread))
				nSyncThreads++;
			nSyncs++;
			if (evt.posInHistory)
				nSyncsHist++;			
			//currentRequests.add(evt);
			
			evt.thread.lockOps.add(evt);
		}
		else if (evt.type == EventType.YIELD) {
			nYields++;
			((YieldEvent)evt).matchingTemplate.nYields++;
		}
		/*else if (evt.type == EventType.WAKE_UP) {
			for (int i = 0; i < currentYields.size(); i++) {
				if (currentYields.get(i).thread == evt.thread) {
					tYields += evt.time- currentYields.get(i).time;
					currentYields.removeFast(i);
					break;//there is only one yield at a time for a given thread
				}
			}
		}*/
		/*else if (evt.type == EventType.GRANT) {
			for (int i = 0; i < currentRequests.size(); i++) {
				if (currentRequests.get(i).thread == evt.thread) {
					tAvoidances += evt.time- currentRequests.get(i).time;
					currentRequests.removeFast(i);
					break;//there is only one request at a time for a given thread
				}
			}
		}*/
	}
	
	void refreshStats() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long m = memBean.getHeapMemoryUsage().getUsed()+ memBean.getNonHeapMemoryUsage().getUsed();            
        if (m > peakMemConsumption)
        	peakMemConsumption = m;
        
        //calculate average depth
        int ndepths = 0;
        int sumdepths = 0;
        for (Signature tmpl: dImmunix.history)
        	for (SignaturePosition tpos: tmpl.positions) {
        		ndepths++;
        		sumdepths += tpos.depth;
        	}
        if (ndepths > 0)
        	avgDepth = (float)sumdepths/ ndepths;
        else
        	avgDepth = 0;
        
        //refresh number of history positions
        countNewHistoryPositions();
	}
	
	void countNewHistoryPositions() {
		for (Signature tmpl: dImmunix.history) 
			for (SignaturePosition p: tmpl.positions) {
				if (!positionsInHist.containsRef(p.value))
					positionsInHist.add(p.value);
			}
		nPositionsHist = positionsInHist.size();
	}

	void updateLog() {
		float durationSec = ((float)(System.nanoTime()- dImmunix.startTime))/ 1000/ 1000/ 1000;
		
		if (dImmunix.experimentLevel == Dimmunix.EVENTS0) {
			nSyncThreads = 0;
			nSyncs = 0;
			for (int i = 0; i < dImmunix.rag.threads.size(); i++) {
				ThreadNode t = dImmunix.rag.threads.get(i); 
				if (t.nSyncs > 0) {
					nSyncThreads++;
					nSyncs += t.nSyncs;
				}
			}
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(dImmunix.logFile));
			bw.write("nAppThreads="+ dImmunix.appThreads.size()+ ", ");
			bw.write("nSyncThreads="+ nSyncThreads+ ", ");
			bw.write("nLocks="+ nLocks+ ", ");
			bw.write("nPositions="+ nPositions+ ", ");
			bw.write("nPositionsHist="+ nPositionsHist+ ", ");
			bw.write("nYields="+ nYields+ ", ");
			bw.write("nYieldsPerSec="+ nYields/ durationSec+ ", ");
			//bw.write("tYieldsSec="+ ((double)tYields/ 1000/ 1000 /1000)+ ", ");
			bw.write("nSyncs="+ nSyncs+ ", ");
			bw.write("nSyncsPerSec="+ nSyncs/ durationSec+ ", ");
			bw.write("nSyncsHist="+ nSyncsHist+ ", ");
			bw.write("nSyncsHistPerSec="+ nSyncsHist/ durationSec+ ", ");
			//bw.write("tAvoidancesSec="+ ((double)tAvoidances/ 1000/ 1000/ 1000)+ System.getProperty("line.separator"));
			bw.write("nFPs="+ FPDetector.nFPs+ System.getProperty("line.separator"));
			bw.write("nTPs="+ FPDetector.nTPs+ System.getProperty("line.separator"));
			bw.write("avgFinalDepths="+ Logger.avgDepth+ System.getProperty("line.separator"));
			
			bw.close();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
