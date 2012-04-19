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

package dimmunix.deadlock;

import dimmunix.Vector;

public class YieldEvent extends Event {
	Signature matchingTemplate = null;
	Vector<Yield> yields = new Vector<Yield>(10);
	
	void clearYields() {
		yields.removeAllElements();
	}
	
	void addYield(ThreadNode t, LockNode l, Position p) {
		yields.add(new Yield(t, l, p));
	}
	
	int avoidanceIndex() {
		for (int i = 0; i < yields.size(); i++) {
			if (yields.get(i).thread == thread)
				return i;
		}
		return -1;
	}	
}
