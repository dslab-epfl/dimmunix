package dimmunix;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import dimmunix.analysis.Analysis;
import dimmunix.analysis.HashAnalysis;
import dimmunix.init.DimmunixInitDlcks;

public class ClassChecker implements ClassFileTransformer {
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		className = className.replace("/", ".");

		if (Configuration.instance.skip(className)) {
			// System.err.println("Skipping "+ className);
			return null;
		}

		synchronized (Analysis.instance) {
			Analysis.instance.loadedClasses.add(className);			
		}
		
		if (Configuration.instance.communixEnabled) {
			if (!HashAnalysis.instance.alreadyComputed(className)) {
				int hash = Util.computeHash(classfileBuffer);
				HashAnalysis.instance.addHash(className, hash);				
			}
		}
		
		if (Configuration.instance.initEnabled) {
			//avoid init deadlocks
			
			DimmunixInitDlcks.instance.beforeInit(className);
		}

		return null;
	}
}
