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

package instrumentation;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class InstrumentationMethodAdapter extends MethodAdapter {

	private int addedInstructions;
	private boolean isStatic;
	private boolean isSynchronised;

	public InstrumentationMethodAdapter(MethodVisitor arg0, boolean sync, boolean stat) {
		super(arg0);
		isStatic = stat;
		isSynchronised = sync;
	}

	private void loadSyncObject() {
		if (isStatic) {
			mv.visitLdcInsn(Type.getObjectType(ClassTransformer.className()));
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}
	}

	public void visitCode() {
		if (!isSynchronised)
			return;
		loadSyncObject();
		insertCode(Opcodes.MONITORENTER, "MonitorEnterBefore");
		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.MONITORENTER);
		insertCode(Opcodes.MONITORENTER, "MonitorEnterAfter");
		mv.visitInsn(Opcodes.POP);
	}

	public void addMonitorExitBeforeReturn() {
		if (!isSynchronised)
			return;
		loadSyncObject();
		insertCode(Opcodes.MONITOREXIT, "MonitorExitBefore");
		mv.visitInsn(Opcodes.MONITOREXIT);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitMaxs(maxStack + addedInstructions, maxLocals);
	}

	private void insertCode(int opcode, String type) {
		mv.visitInsn(Opcodes.DUP);
		mv.visitIntInsn(Opcodes.BIPUSH, opcode);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				"dimmunix/TrackDelegate", "track" + type,
				"(Ljava/lang/Object;B)V");
		addedInstructions += 2;
	}

	@Override
	public void visitInsn(int opcode) {

		switch (opcode) {
		case Opcodes.MONITORENTER:
			insertCode(opcode, "MonitorEnterBefore");
			mv.visitInsn(Opcodes.DUP);
			mv.visitInsn(opcode);
			insertCode(opcode, "MonitorEnterAfter");
			mv.visitInsn(Opcodes.POP);
			break;
		case Opcodes.MONITOREXIT:
			insertCode(opcode, "MonitorExitBefore");
			mv.visitInsn(opcode);
			break;
		case Opcodes.ATHROW:
		case Opcodes.RETURN:
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
			addMonitorExitBeforeReturn();
			mv.visitInsn(opcode);
			break;
		default:
			mv.visitInsn(opcode);
		}

	}
}
