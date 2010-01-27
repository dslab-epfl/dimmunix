package dIV.interf;

import java.util.LinkedList;

import dIV.core.staticAnalysis.Signature;

/**
 * Class that mediates access to the history file
 * Uses the singleton pattern for the "history" variable
 * 
 * @author cristina
 *
 */
final class History implements IReadOnlyHistory, IFullHistory {
	
	private LinkedList<Signature> signatures;
	private static final History history = new History();
	
	private History() {
		signatures = new LinkedList<Signature>();
		
		// TODO get signatures from history file
	}

	@Override
	public synchronized LinkedList<Signature> getSignatures() {
		// TODO Auto-generated method stub
		return signatures;
	}

	@Override
	public synchronized void addSignature(Signature s) {
		// TODO Auto-generated method stub
		signatures.add(s);
	}
	
	@Override
	public synchronized void removeSignature(Signature s) {
		// TODO Auto-generated method stub
		signatures.remove(s);
	}

	public static History getHistory() {
		return history;
	}
}
