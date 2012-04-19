package org.aspectj.weaver.loadtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;

public class SyncPositions {
	public static HashSet<StackTraceElement> syncPositions = new HashSet<StackTraceElement>();
	public static boolean instrumentAll = false;
	public static HashSet<String> classes = new HashSet<String>();
	public static HashSet<String> methods = new HashSet<String>();	
	
	public static void load() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("sync_positions"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("all")) {
					instrumentAll = true;
					break;
				}
				StackTraceElement p = parsePosition(line);
				syncPositions.add(p);
				classes.add(p.getClassName());
				methods.add(p.getMethodName());
			}
			br.close();						
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void add(StackTraceElement syncPos) {
		if (syncPositions.contains(syncPos))
			return;
		
		syncPositions.add(syncPos);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("sync_positions", true));			
			bw.write(syncPos+ System.getProperty("line.separator"));
			new File("weaver_cache/"+ syncPos.getClassName()).delete();
			UnchangedClasses.unchangedClasses.remove(syncPos.getClassName());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static StackTraceElement parsePosition(String pos) {
		StringTokenizer st = new StringTokenizer(pos, "()");
		String tok1 = st.nextToken();
		String tok2 = st.nextToken();

		String declaringClass = tok1.substring(0, tok1.lastIndexOf('.'));
		String methodName = tok1.substring(tok1.lastIndexOf('.') + 1);
		StringTokenizer st2 = new StringTokenizer(tok2, ":");
		String fileName = st2.nextToken();
		int lineNumber = 0;
		if (st2.hasMoreTokens())
			lineNumber = Integer.parseInt(st2.nextToken());

		return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
	}	
}
