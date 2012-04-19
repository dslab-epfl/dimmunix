package dimmunix.profiler;

import java.util.HashSet;

public class ThreadInfo {
	public HashSet<LockInfo> locksHeld = new HashSet<LockInfo>();	
}
