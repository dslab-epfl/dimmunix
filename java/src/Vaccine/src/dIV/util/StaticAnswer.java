package dIV.util;

/**
 * Class representing the verdict given by the Static Analyzer for a signature
 * 
 * @author cristina
 *
 */
public class StaticAnswer {
	private boolean valid;
	
	public StaticAnswer(boolean valid) {
		this.valid = valid;
	}
	
	public boolean isValid() {
		return valid;
	}
}
