package dimmunix;

import java.lang.reflect.Method;

public class SpecialStackTrace {
	private static Method m;
//	private static Method size;

	static {
		try {
			Class cls = Class.forName("java.lang.Throwable");
			m = cls.getDeclaredMethod("getStackTraceElement", int.class);
			m.setAccessible(true);
//			size = cls.getDeclaredMethod("getStackTraceDepth", null);
//			size.setAccessible(true);
		} catch (Throwable t) {
			System.err.println("FAILED!!!");

		}
	}

	public static void getStackTrace(Vector<StackTraceElement> stackTrace, int depth, int startAt) {
		Exception e = new Exception();
		stackTrace.clear();
//		int maxSize = (Integer) size.invoke(e, null);
		for (int i = startAt; i < depth; i++) {
			try {
				StackTraceElement ste = (StackTraceElement) m.invoke(e, i);
				stackTrace.add(ste);
			}
			catch (Throwable ex) {		
				break;
			}
		}
	}
	
	public static void getStackTrace(CallStack stackTrace, int depth, int startAt) {
		Exception e = new Exception();
		stackTrace.clear();
//		int maxSize = (Integer) size.invoke(e, null);
		for (int i = startAt; i < depth; i++) {
			try {
				StackTraceElement ste = (StackTraceElement) m.invoke(e, i);
				stackTrace.add(ste);
			}
			catch (Throwable ex) {		
				break;
			}
		}
	}
	
	public static StackTraceElement getFrame(int offset) {
		Exception e = new Exception();
		try {
			return (StackTraceElement) m.invoke(e, offset);
		}
		catch (Throwable ex) {
			return null;
		}
	}
}
