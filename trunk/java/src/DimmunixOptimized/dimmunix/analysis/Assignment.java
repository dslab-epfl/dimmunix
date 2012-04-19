package dimmunix.analysis;

import soot.Value;

public class Assignment {
	public Value leftValue;
	public Value rightValue;
	
	public Assignment(Value leftValue, Value rightValue) {
		this.leftValue = leftValue;
		this.rightValue = rightValue;
	}	
}
