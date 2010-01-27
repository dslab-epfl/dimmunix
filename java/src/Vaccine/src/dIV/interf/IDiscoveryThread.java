package dIV.interf;

import java.util.LinkedList;

import dIV.util.Peer;

/**
 * Interface for the discovery thread
 * 
 * @author cristina
 *
 */
public interface IDiscoveryThread {
	/**
	 * finds the other peers and stores the result in a list
	 * should be synchronized
	 */
	public void findPeers();
	
	/**
	 * should be static and synchronized
	 */
	public LinkedList<Peer> getPeersList();
}
