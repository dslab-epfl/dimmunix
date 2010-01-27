package dIV.core.staticAnalysis;


/**
 * Class describing a PointsToAnalysis exception
 * 
 * @author cristina (cristina.basescu@gmail.com)
 *
 */
@SuppressWarnings("serial")
public class PTAException extends Exception {
	
	private String string= "A PTA Exception has occured";
	
	public String toString() {
		return string;
	}
	
	public PTAException(String s) {
		string += ": "+s;
	}
	
	public PTAException(){}
}