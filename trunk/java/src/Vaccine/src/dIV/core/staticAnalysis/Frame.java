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

package dIV.core.staticAnalysis;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Class representing a frame
 *
 */
public class Frame {
	private String className;
	private String method;
	private String file;
	private int line;
	
	private String format;
	
	/**
	 * parses a frame in format class(file:line)
	 * @param format
	 */
	public Frame (String format) throws NoSuchElementException, SigFormatException {
		if(format.compareTo("") == 0)
			throw new SigFormatException();
		
		StringTokenizer st = new StringTokenizer(format, "(");
		
		// get className and method
		String classAndMethod = st.nextToken();
		int index = classAndMethod.lastIndexOf(".");
		className = classAndMethod.substring(0, index);
		method = classAndMethod.substring(index+1);
		if(className.compareTo("") == 0 || method.compareTo("") == 0)
			throw new SigFormatException();
		
		// get file and line
		StringTokenizer st2 = new StringTokenizer(st.nextToken(), ":)");
		this.file = st2.nextToken();
		this.line = Integer.parseInt(st2.nextToken());
		
		this.format = format;
	}
	
	/**
	 * builds a new frame, given all the necessary arguments
	 * @param className
	 * @param method
	 * @param file
	 * @param line
	 */
	public Frame(String className, String method, String file, int line) {
		this.className = className;
		this.method = method;
		this.file = file;
		this.line = line;
	}
	
	public String getClassName () {
		return this.className;
	}
	
	public String getMethod () {
		return this.method;
	}
	
	public String getFile () {
		return this.file;
	}
	
	public int getLine () {
		return this.line;
	}
	
	public String getFormat() {
		return this.format;
	}
	
	public void setLine(int line) {
		this.line = line;
	}
	
	public void print() {
		System.out.println("class=" + this.className +" method = " + this.method + " file = " + this.file + " line = " + this.line);
	}
	
	/**
	 * check if current frame matches frame f
	 * @param f
	 */
	public boolean match (Frame f) {
		if(this.getClassName().compareTo(f.getClassName()) == 0
			&& this.getMethod().compareTo(f.getMethod()) == 0
			&& this.getFile().compareTo(f.getFile()) == 0
			&& this.getLine() == f.getLine())
				return true;
		return false;
	}
}
