package dimmunix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class Configuration {
	//classes to skip
	public Vector<String> classesToSkip;
	
	public static final Configuration instance = new Configuration();
	
	public boolean dimmunixEnabled = false;
	public boolean communixEnabled = false;
	public String communixServer = "localhost";
	public boolean condVarEnabled = false;
	public boolean initEnabled = false;
	public boolean hybridEnabled = false;
	public boolean externalEnabled = false;
	public boolean profilerEnabled = false;
	
	Configuration() {
		this.classesToSkip = new Vector<String>();		
	}
	
	public void init() {
		this.loadClassesToSkip();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("Dimmunix.conf"));
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				StringTokenizer stok = new StringTokenizer(line, "= ");
				String key = stok.nextToken();
				String val = stok.nextToken();
				if (key.equals("dimmunixEnabled"))
					this.dimmunixEnabled = Boolean.parseBoolean(val);
				if (key.equals("communixEnabled"))
					this.communixEnabled = Boolean.parseBoolean(val);
				if (key.equals("communixServer"))
					this.communixServer = val;
				else if (key.equals("condVarEnabled"))
					this.condVarEnabled = Boolean.parseBoolean(val);
				else if (key.equals("initEnabled"))
					this.initEnabled = Boolean.parseBoolean(val);
				else if (key.equals("hybridEnabled"))
					this.hybridEnabled = Boolean.parseBoolean(val);
				else if (key.equals("externalEnabled"))
					this.externalEnabled = Boolean.parseBoolean(val);
				else if (key.equals("profilerEnabled"))
					this.profilerEnabled = Boolean.parseBoolean(val);
			}
			br.close();
		} catch (Exception e) {
		}		
	}
	
	public boolean skip(String className) {
		for (String cl : Configuration.instance.classesToSkip) {
			if (className.contains(cl))
				return true;
		}
		return false;
	}
	
	private void loadClassesToSkip() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("skip_classes"));
			String cl;
			while ((cl = br.readLine()) != null) {
				this.classesToSkip.add(cl);
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
