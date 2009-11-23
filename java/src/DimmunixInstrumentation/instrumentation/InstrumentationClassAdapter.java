package instrumentation;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InstrumentationClassAdapter extends ClassAdapter {

	private ClassTransformer clTrans;
	
	public InstrumentationClassAdapter(ClassVisitor arg0, ClassTransformer clTrans) {
		super(arg0);
		// System.out.println("class to visit: "+ClassTransformer.className());
		this.clTrans = clTrans;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv;
		boolean sync = ((access & Opcodes.ACC_SYNCHRONIZED) != 0);
		boolean stat = ((access & Opcodes.ACC_STATIC) != 0);
		//boolean nat = ((access & Opcodes.ACC_NATIVE) != 0); 

		//remove sync modifier if method is not native
		if (sync && clTrans.canRemoveSyncModifier) {
			access ^= Opcodes.ACC_SYNCHRONIZED;
		}
		mv = cv.visitMethod(access, name, desc, signature, exceptions);
		if (name.equals("<init>")) {
			return mv;
		}
		if (mv != null) {
			mv = new InstrumentationMethodAdapter(mv, sync, stat);
		}

		return mv;
	}

}
