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

public class LockNode extends Node {
	int id;
	int nlck = 0;
	
	ThreadNode owner = null;
	Position acqPos = null;
	boolean acqPosInHistory = false;
	
	long grantTime = 0;
	
	public LockNode(int objId) {
		this.id = objId;
	}

	public String toString() {
		return ""+ this.id;
	}
	
	public int hashCode() {
		return this.id;
	}

	public boolean equals(Object n) {
		if (n == null || !(n instanceof LockNode))
			return false;
		return this.id == ((LockNode)n).id;
	}
}
