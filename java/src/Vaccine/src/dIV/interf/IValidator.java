package dIV.interf;

import java.util.LinkedList;

import dIV.core.staticAnalysis.Signature;
import dIV.util.ReputationAnswer;
import dIV.util.ReputationInformation;
import dIV.util.StaticAnswer;


public abstract class IValidator {
	/** gets the history with the proper access level
	 * 
	 * @return the history
	 */
	protected IFullHistory getHistory() {
		return History.getHistory();
	}
	
	public abstract void registerMonitoringSystem (IMonitoringSystem ms);
	
	public abstract void registerReputationSystem (IReputationSystem rs);
	
	public abstract void registerStaticAnalyzer (IStaticAnalyzer sa);
	
	/**
	 * receive new signatures from the receiver daemon
	 * 
	 * @param list the signatures received
	 */
	public abstract void acceptSignatures (LinkedList<Signature> list);
	
	/**
	 * remove a signature from the history (called by the monitoring system)
	 * 
	 * @param s the signature to be removed
	 */
	public abstract void disableSignature (Signature s);
	
	/**
	 * send reputation information to the reputation system (called by the monitoring system)
	 * 
	 * @param ri the information
	 */
	public abstract void sendReputationInformation (ReputationInformation ri);
}