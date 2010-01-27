package dIV.core.staticAnalysis;


import dIV.interf.IMayAliasAnalysis;

import java.util.HashSet;



import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.tagkit.LineNumberTag;
import dIV.util.Properties;

/**
 * Class that performs may alias analysis
 * 
 * @author cristina (cristina.basescu@gmail.com)
 *
 */
public class MayAliasAnalysis implements IMayAliasAnalysis {
	
	// classes on which alias analysis is employed
	private HashSet<String> worksetClasses;
	// main class of the analyzed program
	private String mainClass;
	
	public MayAliasAnalysis(HashSet<String> worksetClasses, String mainClass) {
		this.worksetClasses = new HashSet<String>();
		this.worksetClasses.addAll(worksetClasses);
		this.mainClass = mainClass;
		// read properties
		//Properties.read();
	}

	/** checks whether the value first, used in method m1, may alias value second,
	 * used in method m2
	 * 
	 * @param m1
	 * @param first
	 * @param m2
	 * @param second
	 * @return true or false
	 */
	public boolean mayAlias(SootMethod m1, Value first, SootMethod m2, Value second) {
		
		// check whether first and second are the same value
		if(first.equals(second)) {
			return true;
		} 
		
		// we must not check the compile time type, which may change during runtime
		
		// initialize pointsto analysis, if it hasn't been already initialized
		if(PointsToAnalysis.loadedClasses == null)
			PointsToAnalysis.init(this.worksetClasses, this.mainClass);
		
		try {
			boolean result = PointsToAnalysis.analyze(m1.getDeclaringClass(), m1.getName(), first,
					m2.getDeclaringClass(), m2.getName(), second);
			return result;
		} catch (PTAException e) {
			e.printStackTrace();
		}
		
		// conservative result
		return true;
	}

}
