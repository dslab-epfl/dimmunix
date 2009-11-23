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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ClassTransformer implements ClassFileTransformer {
	// private final ResourceManager manager;

	private static String className;
	boolean canRemoveSyncModifier = true;

	public static String className() {
		return className;
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		ClassTransformer.className = className;
		if (filter()) {
			//System.out.println("class "+className+ " not instrumented");
			return classfileBuffer;
		}
		//System.out.println("class "+className + " to be found in : "+ClassLoader.getSystemResource(className+".class").getFile());
		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new InstrumentationClassAdapter(cw, this);
		cr.accept(cv, 0);
		byte[] res = cw.toByteArray();
//		try {
//			File file = new File("instrumented/" + className.replace("/", ".")
//					+ ".class");
//			OutputStream out = new FileOutputStream(file);
//			out.write(res);
//			out.close();
//			System.out.println("Saved: " + file.getAbsolutePath());
//		} catch (Exception e) {
//			System.out.println("Saving class failed");
//		}
//		System.out.println("class "+className+ " instrumented");
		return res;
	}

	protected boolean filter() {
		return className.startsWith("dimmunix/");
	}
}
