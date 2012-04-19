package dimmunix.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class HashAnalysis {
	//the classes that are already checked, with their hashcode
	private final HashMap<String, Integer> classHashes = new HashMap<String, Integer>();
	
	private HashAnalysis() {		
	}
	
	public static final HashAnalysis instance = new HashAnalysis();
	
	public void init() {
		this.loadHashes();
	}
	
	public void saveResults() {
		this.saveHashes();
	}
	
	public boolean alreadyComputed(String cl) {
		return this.classHashes.containsKey(cl);
	}
	
	public void addHash(String cl, int h) {
		this.classHashes.put(cl, h);
	}
	
	public int getHash(String cl) {
		Integer h = this.classHashes.get(cl);
		if (h == null)
			return 0;
		return h;
	}
	
	private void saveHashes() {
//		System.out.println("saving class hashes");
		try {
			PrintWriter pw = new PrintWriter("class_hashes");
			for (Map.Entry<String, Integer> e : classHashes.entrySet()) {
				pw.write(e.getKey() + " " + e.getValue() + "\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void loadHashes() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("class_hashes"));
			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer stok = new StringTokenizer(line, " ");
				String className = stok.nextToken();
				Integer hash = Integer.parseInt(stok.nextToken());
				classHashes.put(className, hash);
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
