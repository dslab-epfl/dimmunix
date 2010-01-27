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
import dIV.interf.IReadOnlyHistory;
import dIV.util.ReputationInformation;

/**
 * Abstract class for the Monitoring System
 * Controls the operations available on the validator
 * 
 */
public abstract class IMonitoringSystem {
	
	private IValidator validator;
	
	public final void setValidator (IValidator v) {
		this.validator = v;
	}
	
	/** gets the history with the proper access level
	 * 
	 * @return the history
	 */
	protected IReadOnlyHistory getHistory() {
		return History.getHistory();
	}
	
	/** gets the log file with the proper access level
	 * 
	 * @return the log file
	 */
	protected LogFile getLogFile() {
		return LogFile.getLogFile();
	}
	
	/**
	 * remove a signature from the history by passing it to the validator
	 * 
	 * @param s the signature
	 */
	public final void disableSignature (Signature s) {
		validator.disableSignature(s);
	}
	
	/**
	 * send computed reputation information to the reputation system by passing it to the validator
	 * 
	 * @param ri the information
	 */
	public final void sendReputationInformation (ReputationInformation ri) {
		validator.sendReputationInformation(ri);
	}
	
	/**
	 * the logic for monitoring signatures
	 */
	public abstract void monitorSignatures();
}
