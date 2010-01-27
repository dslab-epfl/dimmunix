package dIV.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

public class Properties {
	public static final int SPARK = 1;
	public static final int PADDLE = 2;

	// currently used pointsTo framework
	private static int ptFramework;
	private static String k = "1";
	private static String context = "insens";
	private static String contextHeap = "false";
	private static String sigFile = "sig";
	private static boolean printInfo = false;
	
	public static int getPTFramework() {
		return ptFramework;
	}
	
	public static String getK() {
		return k;
	}
	
	public static String getContext() {
		return context;
	}
	
	public static String getContextHeap() {
		return contextHeap;
	}
	
	public static String getSigFile() {
		return sigFile;
	}
	
	public static boolean getPrintInfo() {
		return printInfo;
	}
	
	public static void read() {
		String filename = "properties";
		
		// read the properties
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader(filename));
		
			String line = "";
			while ( (line = br.readLine()) != null) {
				if(line.compareTo("") != 0 && !line.startsWith(";")) {
					// parse contents
					StringTokenizer st = new StringTokenizer(line, "=");
					String id = st.nextToken();
					
					if(id.startsWith("PRINT_INFO")) {
						String token = st.nextToken();
						if(token.compareTo("on") == 0)
							printInfo = true;
						else
							if(token.compareTo("off") == 0)
								printInfo = false;
					}
					
					if(id.startsWith("POINTSTO_FRAMEWORK")) {
						if(st.nextToken().startsWith("SPARK"))
							ptFramework = SPARK;
						else
							ptFramework = PADDLE;
					}
					
					if(id.startsWith("PADDLE_K")) {
						k = st.nextToken();
					}
					
					if(id.startsWith("PADDLE_CONTEXT")) {
						context = st.nextToken();
					}
					
					if(id.startsWith("PADDLE_CONTEXT_HEAP")) {
						contextHeap = st.nextToken();
					}
					
					if(id.startsWith("SIGNATURE_FILE_NAME")) {
						sigFile = st.nextToken();
					}
					
					
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
}