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

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import dimmunix.Dimmunix;
import dimmunix.TrackDelegate;

public class InstrumentationMain {
	
	private static String[] classesToRedefine = new String[]{"java.util.Vector", "java.lang.StringBuffer", "java.util.Hashtable"};
	
	public static void premain(String agentArguments,
			Instrumentation instrumentation) {
		try {
			ClassLoader.getSystemClassLoader().loadClass("dimmunix.TrackDelegate");
			TrackDelegate.dimmunix = new Dimmunix();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		ClassTransformer clTrans = new ClassTransformer();
		instrumentation.addTransformer(clTrans, true);
		
		Class[] classes = new Class[classesToRedefine.length];
		for (int i = 0; i < classes.length; i++) {
			try {
				classes[i] = ClassLoader.getSystemClassLoader().loadClass(classesToRedefine[i]);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		try {
			clTrans.canRemoveSyncModifier = false;
			instrumentation.retransformClasses(classes);
			clTrans.canRemoveSyncModifier = true;
		} catch (UnmodifiableClassException e1) {
			e1.printStackTrace();
		}
	}
}
