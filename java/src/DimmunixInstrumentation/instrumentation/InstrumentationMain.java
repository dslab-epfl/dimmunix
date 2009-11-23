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
