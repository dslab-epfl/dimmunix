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
