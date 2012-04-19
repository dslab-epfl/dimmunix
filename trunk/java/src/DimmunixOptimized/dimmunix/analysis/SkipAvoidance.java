package dimmunix.analysis;

public class SkipAvoidance {
	public StackTraceElement position;
	public int sigId;
	public int index;
	public boolean before;
	
	public SkipAvoidance(StackTraceElement position, int sigId, int index, boolean before) {
		this.position = position;
		this.sigId = sigId;
		this.index = index;
		this.before = before;
	}
	
	public String toString() {
		return position+ " "+ sigId+ " "+ index+ " "+ before;
	}
	
	public int hashCode() {
		int b = 0;
		if (before)
			b = 1;
		return position.hashCode()^ sigId^ index^ b;
	}

	public boolean equals(Object obj) {
		SkipAvoidance other = (SkipAvoidance)obj;
		if (other == null)
			return false;
		return other.position.equals(this.position) && other.sigId == this.sigId && other.index == this.index && other.before == this.before;
	}	
}
