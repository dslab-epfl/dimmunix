package dIV.interf;

import java.util.LinkedList;

import dIV.core.staticAnalysis.Signature;

/**
 * Abstract class for the receiver daemon
 * Controls the operations available on the validator
 * 
 * @author cristina
 *
 */
public abstract class IReceiverDaemon {
	
	private IValidator validator;
	
	public final void setValidator (IValidator v) {
		this.validator = v;
	}
	
	/**
	 * receives a signature from some other peer
	 * 
	 * @param s the signature received
	 */
	public abstract void receiveSignature (Signature s);
	
	/**
	 * send received signatures to the validator
	 */
	public final void sendSignatures (LinkedList<Signature> list) {
		validator.acceptSignatures(list);
	}
}
