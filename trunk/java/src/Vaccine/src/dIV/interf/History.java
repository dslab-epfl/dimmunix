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

/**
 * Class that mediates access to the history file
 * Uses the singleton pattern for the "history" variable
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
