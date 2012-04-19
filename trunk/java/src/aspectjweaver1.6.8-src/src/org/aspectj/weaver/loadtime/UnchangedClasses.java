package org.aspectj.weaver.loadtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

public class UnchangedClasses {
	public static HashSet<String> unchangedClasses = new HashSet<String>();
	
	public static void save() {		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("unchanged_classes"));
			synchronized (unchangedClasses) {
				for (String cl: unchangedClasses) {
					bw.write(cl+ System.getProperty("line.separator"));
				}				
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
	
	public static void load() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("unchanged_classes"));
			String line;
			while ((line = br.readLine()) != null) {
				unchangedClasses.add(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
