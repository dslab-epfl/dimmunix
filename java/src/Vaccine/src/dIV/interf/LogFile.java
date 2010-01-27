package dIV.interf;

import java.util.LinkedList;

import dIV.util.Log;

/**
 * Class that mediates access to the log file
 * Uses the singleton pattern for the "logFile" variable
 * @author cristina
 *
 */
public final class LogFile {
	private String logFileName;
	private static final LogFile logFile = new LogFile("");
	private LinkedList<Log> logList;
	
	private LogFile(String logFileName) {
		this.logFileName = logFileName;
		this.logList = new LinkedList<Log>();
	}
	
	/**
	 * gets log information from the log file
	 * 
	 */
	public LinkedList<Log> getLogs() {
		// TODO implement
		return null;
	}
	
	public static LogFile getLogFile() {
		return logFile;
	}
	
}
