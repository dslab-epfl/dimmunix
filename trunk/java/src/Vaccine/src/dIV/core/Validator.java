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

package dIV.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import dIV.core.staticAnalysis.Signature;
import dIV.core.staticAnalysis.StaticAnalysis;
import dIV.interf.IMonitoringSystem;
import dIV.interf.IReputationSystem;
import dIV.interf.IStaticAnalyzer;
import dIV.interf.IValidator;
import dIV.util.ReputationAnswer;
import dIV.util.ReputationInformation;
import dIV.util.StaticAnswer;
import dIV.util.Properties;

/**
 * Class representing the validator
 * 
 */
public class Validator extends IValidator {

	public IMonitoringSystem monitoringSystem;
	public IReputationSystem reputationSystem;
	public IStaticAnalyzer staticAnalyzer;

	@Override
	public void registerMonitoringSystem(IMonitoringSystem ms) {
		this.monitoringSystem = ms;
		ms.setValidator(this);
	}

	@Override
	public void registerReputationSystem(IReputationSystem rs) {
		this.reputationSystem = rs;
		rs.setValidator(this);
	}

	@Override
	public void registerStaticAnalyzer(IStaticAnalyzer sa) {
		this.staticAnalyzer = sa;
		sa.setValidator(this);
	}

	@Override
	public void acceptSignatures(LinkedList<Signature> list) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disableSignature(Signature s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendReputationInformation(ReputationInformation ri) {
		reputationSystem.updateReputationInformation(ri);
	}
}
