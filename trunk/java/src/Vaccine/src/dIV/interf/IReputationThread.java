/*
     Created by Saman A. Zonouz, Horatiu Jula, Pinar Tozun, Cristina Basescu, George Candea
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix Vaccination Framework.

     Dimmunix Vaccination Framework is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix Vaccination Framework is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix Vaccination Framework. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

package dIV.interf;

import dIV.util.ReputationInformation;

/**
 * Interface for the reputation thread
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
