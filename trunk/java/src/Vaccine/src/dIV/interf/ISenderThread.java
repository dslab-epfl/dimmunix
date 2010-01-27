package dIV.interf;

import dIV.core.staticAnalysis.Signature;

/**
 * Abstract class for the sender thread
 * 
 * @author cristina
 *
 */
public abstract class ISenderThread {
	
	/** gets the history with the proper access level
	 * 
	 * @return the history
	 */
	protected IReadOnlyHistory getHistory() {
		return History.getHistory();
	}
	
	/**
	 * sends a signature to the other peers
	 * 
	 * @param s the signature to be sent
	 */
	public abstract void sendSignature (Signature s);
}
