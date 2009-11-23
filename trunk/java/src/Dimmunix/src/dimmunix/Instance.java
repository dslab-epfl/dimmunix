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


public class Instance {
	Vector<LockGrant> lockGrants = new Vector<LockGrant>(10);
	Signature template = null;
	int avoidanceIndex;

	boolean isDisjointUntil(int index, LockGrant lg) {
		for (int i = 0; i < index; i++) {			
			if (!lockGrants.get(i).disjoint(lg))
				return false;
		}						
		return true;
	}
	
	void set(int i, LockGrant lg) {
		lockGrants.set(i, lg);
	}
	
	int size() {
		return template.size();
	}
	
	boolean contains(LockGrant lg) {
		for (int i = 0; i < this.size(); i++) {
			if (lockGrants.get(i) == lg)
				return true;
		}
		return false;
	}
	
	boolean isActive(ThreadNode t) {
		for (int i = 0; i < this.size(); i++) {
			if (lockGrants.get(i).thread != t && lockGrants.get(i).n == 0)
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			sb.append("("+ lockGrants.get(i).thread+ ",");
			sb.append(lockGrants.get(i).lock+ ",");
			sb.append(lockGrants.get(i).n+ ",");
			sb.append(lockGrants.get(i).position + ") ");
		}
		return sb.toString();
	}
	
	void setSize(int newSize) {
		this.lockGrants.setSize(newSize);
	}
	
	void clear() {
		this.lockGrants.clear();
		this.template = null;
	}
	
	boolean isEmpty() {
		return this.template == null;
	}	
}
