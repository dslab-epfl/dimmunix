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
 * @author cristina
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
