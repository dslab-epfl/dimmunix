/*
     Created by Saman A. Zonouz, Horatiu Jula, Pinar Tozun, Cristina Basescu, George Candea
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix Vaccination Framework.

     Dimmunix Vaccination Framework is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix Vaccination Framework is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix Vaccination Framework. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

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