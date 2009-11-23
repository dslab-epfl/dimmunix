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

public class TrackDelegate {

	public static Dimmunix dimmunix;
	private static boolean[] inDimmunix = new boolean[100000];
	
	public static void trackMonitorEnterBefore(Object o, byte opcode) {
		Thread t = Thread.currentThread();
		if (inDimmunix[(int)t.getId()])
			return;
		inDimmunix[(int)t.getId()] = true;
		dimmunix.avoidance(t, o);
		inDimmunix[(int)t.getId()] = false;		
	}

	public static void trackMonitorEnterAfter(Object o, byte opcode) {
		Thread t = Thread.currentThread();
		if (inDimmunix[(int)t.getId()])
			return;
		inDimmunix[(int)t.getId()] = true;
		dimmunix.acquire(t, o);
		inDimmunix[(int)t.getId()] = false;		
	}

	public static void trackMonitorExitBefore(Object o, byte opcode) {
		Thread t = Thread.currentThread();
		if (inDimmunix[(int)t.getId()])
			return;
		inDimmunix[(int)t.getId()] = true;
		dimmunix.release(t, o);
		inDimmunix[(int)t.getId()] = false;		
	}
}
