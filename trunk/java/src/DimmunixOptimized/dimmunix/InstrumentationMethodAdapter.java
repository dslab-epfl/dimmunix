/*
     Created by Horatiu Jula, Silviu Andrica, George Candea
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

import java.util.HashSet;

import org.aspectj.weaver.loadtime.ExecutionPositions;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import dimmunix.condvar.DimmunixCondVar;
import dimmunix.deadlock.DimmunixDeadlock;
import dimmunix.deadlock.Signature;

public class InstrumentationMethodAdapter extends MethodAdapter {

//	private int addedInstructions;
	private String className;
	private String methodName;
	private int curLine = 0;
	private boolean isSync;
	
	private HashSet<MatchPosition> inlineCallStackMatchingPositions = new HashSet<MatchPosition>();
	
	private HashSet<Integer> matchedExecutionPositions = new HashSet<Integer>(); 

	public InstrumentationMethodAdapter(MethodVisitor mv, String className, String methodName, boolean isSync) {
		super(mv);
//		this.addedInstructions = 0;
		this.className = className;
		this.methodName = methodName;
		this.isSync = isSync;
	}

	public void visitLineNumber(int line, Label start) {
		this.curLine = line;
		
		mv.visitLineNumber(line, start);
		
		//code for skipping avoidance
		Vector<Pair<Integer, Integer>> sigsToSkipBefore = DimmunixDeadlock.instance.getIdsSignaturesToSkip(className, methodName, curLine, true);
		for (Pair<Integer, Integer> sig: sigsToSkipBefore) {
			mv.visitLdcInsn(sig.v1);
			mv.visitLdcInsn(sig.v2);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dimmunix/deadlock/DimmunixDeadlock", "skipAvoidance", "(II)V");
		}			
	}
	
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitMaxs(maxStack, maxLocals);
	}

	public void visitInsn(int opcode) {
		mv.visitInsn(opcode);
		
		if (opcode == Opcodes.MONITORENTER) {
			Vector<Pair<Integer, Integer>> sigsToSkipAfter = DimmunixDeadlock.instance.getIdsSignaturesToSkip(className, methodName, this.curLine, false);
			for (Pair<Integer, Integer> sig: sigsToSkipAfter) {
				mv.visitLdcInsn(sig.v1);
				mv.visitLdcInsn(sig.v2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dimmunix/deadlock/DimmunixDeadlock", "skipAvoidance", "(II)V");
			}
		}
	}
	
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		if (DimmunixDeadlock.instance.inlineMatching) { 
			//code to match sigs inline
			for (Signature sig: DimmunixDeadlock.instance.history.historyQueue) {
				for (int i = 0; i < sig.size(); i++) {
					CallStack stack = sig.positions.get(i).value.callStack; 
					for (int d = stack.size()- 1; d >= 0; d--) {
						MatchPosition mpos = new MatchPosition(className, methodName, curLine, sig.id, i, d+ 1);
						if (!this.inlineCallStackMatchingPositions.contains(mpos) && this.matchFrame(className, methodName, curLine, stack.get(d))) {
							mv.visitLdcInsn((Integer)sig.id);
							mv.visitLdcInsn((Integer)i);
							mv.visitLdcInsn((Integer)(d+ 1));
							mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dimmunix/deadlock/DimmunixDeadlock", "match", "(III)V");
							this.inlineCallStackMatchingPositions.add(mpos);
//							System.out.println("method "+ className+ ":"+ curLine+ " index "+ i+ " sigId "+ sig.id+ " depth "+ d);
						}
					}
				}
			}
		}
		
		if (DimmunixCondVar.instance.inlineMatching) {
			for (int i = 0; i < ExecutionPositions.instance.getPositions().size(); i++) {
				StackTraceElement p = ExecutionPositions.instance.getPositions().get(i);
				if (!this.matchedExecutionPositions.contains(i) && this.matchFrame(className, methodName, curLine, p)) {
					mv.visitLdcInsn((Integer)i);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/aspectj/weaver/loadtime/ExecutionPositions", "matchPosition", "(I)V");
					this.matchedExecutionPositions.add(i);
				}
			}
		}
		
		mv.visitMethodInsn(opcode, owner, name, desc);
	}

	boolean matchFrame(String className, String methodName, int line, StackTraceElement frame) {
		return frame.getLineNumber() == line && frame.getClassName().equals(className) && frame.getMethodName().equals(methodName);
	}
}
