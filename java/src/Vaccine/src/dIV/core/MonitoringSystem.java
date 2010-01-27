package dIV.core;

import dIV.core.staticAnalysis.Signature;
import dIV.interf.IMonitoringSystem;
import dIV.interf.IReadOnlyHistory;
import dIV.interf.IValidator;
import dIV.interf.LogFile;
import dIV.util.ReputationInformation;

/**
 * Class representing the Monitoring System
 * 
 * @author cristina
 *
 */
public class MonitoringSystem extends IMonitoringSystem {
	
	private IReadOnlyHistory history = getHistory();
	private LogFile logFile = getLogFile();
	
	@Override
	public void monitorSignatures() {
		// TODO Auto-generated method stub
		
	}

}
