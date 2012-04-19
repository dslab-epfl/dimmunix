package org.aspectj.weaver.loadtime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class ExecutionPositions {
	public static final ExecutionPositions instance = new ExecutionPositions();
	
	private final Vector<StackTraceElement> positions = new Vector<StackTraceElement>();
	
	private static final int N_THREADS = 10000;
	
	private final StackTraceElement[] currentPositions = new StackTraceElement[N_THREADS];
	
	private ExecutionPositions() {
		for (int i = 0; i < N_THREADS; i++) {
			this.currentPositions[i] = null;
		}
	}
	
	public StackTraceElement getCurrentPosition(long threadId) {
		return this.getCurrentPosition(threadId, true);
	}
	
	public StackTraceElement getCurrentPosition(long threadId, boolean reset) {
		int tid = (int)threadId;
		
		StackTraceElement pos = this.currentPositions[tid];
		if (reset) {
			this.currentPositions[tid] = null;			
		}
		
		return pos;
	}
	
	public static void matchPosition(int posId) {
		int tid = (int)Thread.currentThread().getId();
		
		instance.currentPositions[tid] = instance.positions.get(posId);		
	}
	
	public void addPosition(StackTraceElement pos) {
		if (!this.positions.contains(pos)) {
			this.positions.add(pos);			
		}
	}
	
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("execution_positions"));			
			String line;			
			
			while ((line = br.readLine()) != null) {
				this.positions.add(Util.parsePosition(line));
			}
			
			br.close();
		} catch (Exception e) {
		}		
	}
	
	public void save() {
		try {
			PrintWriter pw = new PrintWriter("execution_positions");
			
			for (StackTraceElement pos: this.positions) {
				pw.println(pos);				
			}
			
			pw.close();
		} catch (Exception e) {
		}
	}
	
	public List<StackTraceElement> getPositions() {
		return Collections.unmodifiableList(this.positions);
	}
}
