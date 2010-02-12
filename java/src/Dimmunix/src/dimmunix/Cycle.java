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

public class Cycle {
	Vector<Node> nodes = new Vector<Node>(10);
	Vector<Position> positions = new Vector<Position>(10);
	
	public Cycle() {
	}
	
	public Cycle(int capacity) {
		nodes = new Vector<Node>(capacity);
		positions = new Vector<Position>(capacity);
	}
	
	void add(Node node, Position position) {
		this.nodes.add(node);
		this.positions.add(position);
	}
	
	void remove() {
		this.nodes.remove();
		this.positions.remove();
	}
	
	boolean equalsFrom(Cycle cycle, int startIndex) {
		for (int i = 0, k = startIndex; i < this.size(); i++, k = (k+1)% this.size()) {
			if (nodes.get(k) != cycle.nodes.get(i) || positions.get(k) != cycle.positions.get(i))
				return false;
		}
		return true;
	}	

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Cycle))
			return false;
		Cycle cycle = (Cycle)obj;
		if (this.size() != cycle.size())
			return false;
		for (int i = 0; i < this.size(); i++) {
			if (this.equalsFrom(cycle, i))
				return true;
		}
		return false;
	}	
	
	int size() {
		return nodes.size();
	}
	
	boolean isYieldEdge(int i) {
		return (nodes.get(i) instanceof ThreadNode) && (nodes.get((i+ 1)% size()) instanceof ThreadNode);
	}
	
	boolean isHoldEdge(int i) {
		return (nodes.get(i) instanceof LockNode) && (nodes.get((i+ 1)% size()) instanceof ThreadNode);		
	}
	
	boolean isRequestEdge(int i) {
		return (nodes.get(i) instanceof ThreadNode) && (nodes.get((i+ 1)% size()) instanceof LockNode);		
	}
	
	boolean isDeadlock() {
		for (int i = 0; i < size(); i++) {
			if (isYieldEdge(i))
				return false;
		}
		return true;
	}	
	
	void clear() {
		nodes.clear();
		positions.clear();
	}
	
	boolean contains(Node x) {
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i) == x)
				return true;
		}
		return false;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++)
			sb.append(nodes.get(i)+ "-->"+ nodes.get((i+ 1)% size())+ " ");
		return sb.toString();
	}	
}
