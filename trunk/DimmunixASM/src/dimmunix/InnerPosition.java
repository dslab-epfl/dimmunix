package dimmunix;

public class InnerPosition {
	Vector<String> callStack = new Vector<String>(10);

	public InnerPosition(Vector<String> callStack) {
		this.callStack = callStack;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < callStack.size(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(callStack.get(i));			
		}
		return sb.toString();
	}
}
