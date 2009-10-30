package dimmunix;

public class TrackDelegate {

	public static Dimmunix dimmunix;
	private static boolean[] inDimmunix = new boolean[100000];
	
	public static void trackMonitorEnterBefore(Object o, byte opcode) {
		Thread t = Thread.currentThread();
		if (inDimmunix[(int)t.getId()])
			return;
		inDimmunix[(int)t.getId()] = true;
		dimmunix.avoidance(t, o);
		inDimmunix[(int)t.getId()] = false;		
	}

	public static void trackMonitorEnterAfter(Object o, byte opcode) {
		Thread t = Thread.currentThread();
		if (inDimmunix[(int)t.getId()])
			return;
		inDimmunix[(int)t.getId()] = true;
		dimmunix.acquire(t, o);
		inDimmunix[(int)t.getId()] = false;		
	}

	public static void trackMonitorExitBefore(Object o, byte opcode) {
		Thread t = Thread.currentThread();
		if (inDimmunix[(int)t.getId()])
			return;
		inDimmunix[(int)t.getId()] = true;
		dimmunix.release(t, o);
		inDimmunix[(int)t.getId()] = false;		
	}
}
