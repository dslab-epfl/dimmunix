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