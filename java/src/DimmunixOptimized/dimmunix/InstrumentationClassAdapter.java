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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InstrumentationClassAdapter extends ClassAdapter {

	private String className;
	
	public InstrumentationClassAdapter(ClassVisitor arg0, String className) {
		super(arg0);
		this.className = className;
		// System.out.println("class to visit: "+ClassTransformer.className());
//		this.clTrans = clTrans;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv;
		boolean sync = ((access & Opcodes.ACC_SYNCHRONIZED) != 0);
//		boolean stat = ((access & Opcodes.ACC_STATIC) != 0);
		//boolean nat = ((access & Opcodes.ACC_NATIVE) != 0); 

		//remove sync modifier if method is not native
//		if (sync && clTrans.canRemoveSyncModifier) {
//			access ^= Opcodes.ACC_SYNCHRONIZED;
//		}
		mv = cv.visitMethod(access, name, desc, signature, exceptions);
//		if (name.equals("<init>")) {
//			return mv;
//		}
		if (mv != null) {
			mv = new InstrumentationMethodAdapter(mv, className, name, sync);
		}

		return mv;
	}

}
