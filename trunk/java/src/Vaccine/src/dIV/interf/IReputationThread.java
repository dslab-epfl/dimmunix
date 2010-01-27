package dIV.interf;

import dIV.util.ReputationInformation;

/**
 * Interface for the reputation thread
 * 
 * @author cristina
 *
 */
public interface IReputationThread {
	
	/**
	 * called by the reputation system
	 * 
	 * @return the global reputation, computed from the other peers
	 */
	public ReputationInformation getGlobalReputation();
	
	public void setReputationSystem (IReputationSystem rs);
	
	/**
	 * sends the local reputation to the other peers
	 * 
	 * @param ri the information
	 */
	public void sendLocalReputation (ReputationInformation ri);
	
	/**
	 * receives local reputation of other peers
	 * 
	 * @param ri the information
	 */
	public void receiveReputation (ReputationInformation ri);
}
