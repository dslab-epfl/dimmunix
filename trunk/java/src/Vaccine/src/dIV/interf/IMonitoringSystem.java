package dIV.interf;

import dIV.core.staticAnalysis.Signature;
import dIV.interf.IReadOnlyHistory;
import dIV.util.ReputationInformation;

/**
 * Abstract class for the Monitoring System
 * Controls the operations available on the validator
 * 
 * @author cristina
 *
 */
public abstract class IMonitoringSystem {
	
	private IValidator validator;
	
	public final void setValidator (IValidator v) {
		this.validator = v;
	}
	
	/** gets the history with the proper access level
	 * 
	 * @return the history
	 */
	protected IReadOnlyHistory getHistory() {
		return History.getHistory();
	}
	
	/** gets the log file with the proper access level
	 * 
	 * @return the log file
	 */
	protected LogFile getLogFile() {
		return LogFile.getLogFile();
	}
	
	/**
	 * remove a signature from the history by passing it to the validator
	 * 
	 * @param s the signature
	 */
	public final void disableSignature (Signature s) {
		validator.disableSignature(s);
	}
	
	/**
	 * send computed reputation information to the reputation system by passing it to the validator
	 * 
	 * @param ri the information
	 */
	public final void sendReputationInformation (ReputationInformation ri) {
		validator.sendReputationInformation(ri);
	}
	
	/**
	 * the logic for monitoring signatures
	 */
	public abstract void monitorSignatures();
}
