package dimmunix.profiler;

public class LockInfo {
	public int id;
	
	public LockInfo(int id) {
		this.id = id;
	}
	
	public int n = 0;
	public StackTraceElement acqPos = null;
	public LockPositionInfo acqPosInfo = null;
	
	public volatile long tAcq = 0;
	public volatile long tSync = 0;
	
	public int hashCode() {
		return id;
	}
	
	public boolean equals(Object x) {
		return this.id == ((LockInfo)x).id;
	}
}
