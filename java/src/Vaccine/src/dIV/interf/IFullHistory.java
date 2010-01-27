package dIV.interf;

import dIV.core.staticAnalysis.Signature;

/**
 * Interface for full access on the history file
 * 
 * @author cristina
 *
 */
public interface IFullHistory extends IReadOnlyHistory {
	public void addSignature(Signature s);
	public void removeSignature(Signature s);
}
