package dIV.interf;

import java.util.LinkedList;

import dIV.core.staticAnalysis.Signature;

/**
 * Interface for read-only access on the history file
 * 
 * @author cristina
 *
 */
public interface IReadOnlyHistory {
	public LinkedList<Signature> getSignatures();
}
