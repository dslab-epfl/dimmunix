package dIV.interf;

import dIV.core.staticAnalysis.Signature;
import dIV.util.StaticAnswer;

/**
 * Abstract class for the Static Analyzer
 * 
 * 
 * @author cristina
 *
 */
public abstract class IStaticAnalyzer {
	
	private IValidator validator;
	
	public final void setValidator (IValidator v) {
		this.validator = v;
	}
	
	/**
	 * start checking a signature (called by the validator)
	 * 
	 * @param s the signature to be checked
	 */
	public abstract StaticAnswer checkSignature (Signature s);
}
