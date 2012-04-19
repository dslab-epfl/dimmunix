package dimmunix;

public class MatchPosition {
	String className;
	String methodName;
	int lineNumber;
	int sigId;
	int index;
	int depth;
	
	public MatchPosition(String className, String methodName, int lineNumber, int sigId, int index, int depth) {
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.sigId = sigId;
		this.index = index;
		this.depth = depth;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + depth;
		result = prime * result + index;
		result = prime * result + lineNumber;
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + sigId;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MatchPosition other = (MatchPosition) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (depth != other.depth)
			return false;
		if (index != other.index)
			return false;
		if (lineNumber != other.lineNumber)
			return false;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (sigId != other.sigId)
			return false;
		return true;
	}
	
	
}
