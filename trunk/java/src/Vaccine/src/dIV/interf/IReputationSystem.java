package dIV.interf;

import dIV.core.staticAnalysis.Signature;
import dIV.util.ReputationAnswer;
import dIV.util.ReputationInformation;

/**
 * Abstract class for the Reputation System
 * Controls the operations available on the validator
 * 
 * @author cristina
 *
 */
public abstract class IReputationSystem {

	private IValidator validator;
	
	public final void setValidator (IValidator v) {
		this.validator = v;
	}
	
	/**
	 * check a signature (called by the validator)
	 * 
	 * @param s the signature to be checked
	 * @return the answer
	 */
	public abstract ReputationAnswer checkSignature (Signature s);
	
	/**
	 * updates the local reputation information (called by the validator)
	 * 
	 * @param ri the update information
	 */
	public abstract void updateReputationInformation (ReputationInformation ri);
	
	/**
	 * called by the reputation thread
	 * 
	 * @return the local stored reputation
	 */
	public abstract ReputationInformation getLocalReputation ();
	
	public abstract void setReputationThread (IReputationThread rt);
	
}
