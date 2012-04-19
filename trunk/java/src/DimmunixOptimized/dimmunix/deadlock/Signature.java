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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import communix.CallStack;
import communix.Frame;

import dimmunix.Vector;
import dimmunix.analysis.HashAnalysis;


public class Signature {
	public Vector<SignaturePosition> positions = new Vector<SignaturePosition>(10);
	public Vector<InnerPosition> innerPositions = new Vector<InnerPosition>(10);
	public boolean isDeadlockTemplate;
	
	private static int nextId = 0;
	public int id;
	
	int nYields = 0;
	int nYieldCycles = 0;
	
//	ConcurrentLinkedQueue<ThreadNode> threadsToSkip = new ConcurrentLinkedQueue<ThreadNode>();
	ConcurrentLinkedQueue<Instance> currentYields = new ConcurrentLinkedQueue<Instance>();
	
	public BoundedSemaphore lock = new BoundedSemaphore(1);
	
	// <-communix
	AtomicInteger nTP = new AtomicInteger(0);
	AtomicInteger nFP = new AtomicInteger(0);
	// ->

	public Signature(boolean isDeadlockTemplate) {
		this.isDeadlockTemplate = isDeadlockTemplate;
		this.id = nextId;
		nextId++;
	}

	public Signature(boolean isDeadlockTemplate, int id) {
		this.isDeadlockTemplate = isDeadlockTemplate;
		this.id = id;
		if (id >= nextId) {
			nextId = id+ 1;
		}
	}

	public void add(SignaturePosition p) {
		positions.add(p);
	}
	
	public void addInner(InnerPosition p) {
		innerPositions.add(p);
	}
	
	boolean equalsFrom(Signature tmpl, int startIndex) {
		for (int i = 0, k = startIndex; i < this.size(); i++, k = (k+1)% this.size()) {
			if (!positions.get(k).equals(tmpl.positions.get(i)))
				return false;
		}
		return true;
	}	

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Signature))
			return false;
		Signature tmpl = (Signature)obj;
		if (this.size() != tmpl.size())
			return false;
		for (int i = 0; i < this.size(); i++) {
			if (this.equalsFrom(tmpl, i))
				return true;
		}
		return false;
	}	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		if (isDeadlockTemplate)
			sb.append("deadlock_template=");
		else
			sb.append("livelock_template=");
		sb.append(this.id);
		for (int i = 0; i < positions.size(); i++) {
			sb.append(";");
			sb.append(positions.get(i));
			if (!innerPositions.isEmpty())
				sb.append("#"+ innerPositions.get(i));
		}
		return sb.toString();
	}
	
	public int size() {
		return this.positions.size();
	}
	
	public boolean merge(Signature sig) {
		if (this.size() != sig.size()) {
			return false;
		}
		
		Vector<Integer> commonOuter = new Vector<Integer>();		
		for (int i = 0; i < this.size(); i++) {
			SignaturePosition p_this = this.positions.get(i);
			SignaturePosition p_other = sig.positions.get(i);
			if (!p_this.value.callStack.get(0).equals(p_other.value.callStack.get(0))) {
				return false;
			}
			int common = 0;
			for (int d = 0; d < p_this.value.size() && d < p_other.value.size(); d++) {
				if (p_this.value.callStack.get(d).equals(p_other.value.callStack.get(d))) {
					common++;
				}
				else {
					break;
				}
			}
			commonOuter.add(common);
		}
		
		Vector<Integer> commonInner = new Vector<Integer>();		
		for (int i = 0; i < this.size(); i++) {
			InnerPosition p_this = this.innerPositions.get(i);
			InnerPosition p_other = sig.innerPositions.get(i);
			if (!p_this.callStack.get(0).equals(p_other.callStack.get(0))) {
				return false;
			}
			int common = 0;
			for (int d = 0; d < p_this.callStack.size() && d < p_other.callStack.size(); d++) {
				if (p_this.callStack.get(d).equals(p_other.callStack.get(d))) {
					common++;
				}
				else {
					break;
				}
			}
			commonInner.add(common);
		}
		
		for (int i = 0; i < this.size(); i++) {
			this.positions.get(i).shrink(commonOuter.get(i));
			this.innerPositions.get(i).shrink(commonInner.get(i));
		}
		
		return true;
	}
	
	public communix.Signature toCommunixSignature() {
		Vector<communix.CallStack> stacksOuter = new Vector<communix.CallStack>();
		Vector<communix.CallStack> stacksInner = new Vector<communix.CallStack>();
		
		for (SignaturePosition p: this.positions) {
			Vector<Frame> frames = new Vector<Frame>();
			for (StackTraceElement f: p.value.callStack.getFrames()) {
				frames.add(new Frame(f, HashAnalysis.instance.getHash(f.getClassName())));
			}
			stacksOuter.add(new CallStack(frames));
		}
		
		for (InnerPosition p: this.innerPositions) {
			Vector<Frame> frames = new Vector<Frame>();
			for (StackTraceElement f: p.callStack) {
				frames.add(new Frame(f, HashAnalysis.instance.getHash(f.getClassName())));
			}
			stacksInner.add(new CallStack(frames));
		}
		
		return new communix.Signature(stacksOuter, stacksInner);
	}
}
