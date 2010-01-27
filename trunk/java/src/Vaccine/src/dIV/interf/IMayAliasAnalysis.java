package dIV.interf;

import soot.SootMethod;
import soot.Value;

/**
 * Interface for May Alias Analysis
 * 
 * 
 * @author cristina (cristina.basescu@gmail.com)
 *
 */
public interface IMayAliasAnalysis {
	/** checks whether the value first, used in method m1, may alias value second,
	 * used in method m2
	 * 
	 * @param m1
	 * @param first
	 * @param m2
	 * @param second
	 * @return true or false
	 */
	boolean mayAlias(SootMethod m1, Value first, SootMethod m2, Value second);
}
