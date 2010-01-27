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

import dIV.core.staticAnalysis.Signature;
import dIV.util.ReputationAnswer;
import dIV.util.ReputationInformation;

/**
 * Abstract class for the Reputation System
 * Controls the operations available on the validator
 * 
 */
public abstract class IReputationSystem {

	private IValidator validator;
	
	public final void setValidator (IValidator v) {
		this.validator = v;
	}
	
	/**
	 * check a signature (called by the validator)
	 * 
	 * @param s the signature to be checked
	 * @return the answer
	 */
	public abstract ReputationAnswer checkSignature (Signature s);
	
	/**
	 * updates the local reputation information (called by the validator)
	 * 
	 * @param ri the update information
	 */
	public abstract void updateReputationInformation (ReputationInformation ri);
	
	/**
	 * called by the reputation thread
	 * 
	 * @return the local stored reputation
	 */
	public abstract ReputationInformation getLocalReputation ();
	
	public abstract void setReputationThread (IReputationThread rt);
	
}
