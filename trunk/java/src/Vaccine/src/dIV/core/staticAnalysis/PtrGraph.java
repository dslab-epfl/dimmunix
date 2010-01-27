package dIV.core.staticAnalysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


import soot.Local;
import soot.PointsToSet;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.toolkits.graph.BriefUnitGraph;
import dIV.util.Properties;

/**
 * Class representing the pointer graph for a Java class
 * 
 * @author cristina (cristina.basescu@gmail.com)
 *
 */
class PtrGraph {
	
	// edges are from a SootField or a Local to a list of SootFields or Locals
	HashMap<Object,LinkedList<Object>> lists;
	
	soot.PointsToAnalysis pta;
	HashMap<SootClass,Value> loadedClasses;
		
	public PtrGraph(HashMap<SootClass,Value> loadedClasses, soot.PointsToAnalysis pta) {
		lists = new HashMap<Object,LinkedList<Object>>();
		this.pta = pta;
		this.loadedClasses = loadedClasses;
	}
	
	public void setLoadedClasses(HashMap<SootClass,Value> loadedClasses) {
		this.loadedClasses = loadedClasses;
	}
	
	/**
	 * adds an edge
	 * @param from - edge's source 
	 * @param to - edge's destination
	 */
	public void addEdge(Object from, Object to) {
		if(!lists.containsKey(from)) {
			LinkedList<Object> l = new LinkedList<Object>();
			lists.put(from, l);
		}
		lists.get(from).add(to);
	}
	
	private HashMap<Object, PointsToSet> objectsPtsMap = new HashMap<Object, PointsToSet>();

	private PointsToSet getPts(Object o) {
		PointsToSet pts = this.objectsPtsMap.get(o);
		if (pts == null) {
			pts = this.getAnalysisPTS(o);
			this.objectsPtsMap.put(o, pts);
		}
		return pts;
	}
	
	/**
	 *computes the pointsToSet of o according to Soot's PointsToAnalysis result
	 * @param o
	 * @return
	 */
	private PointsToSet getAnalysisPTS(Object o) {
		PointsToSet result = null;
		
		if(o instanceof Local) {
			result = pta.reachingObjects((Local)o);
		}
		else if(o instanceof SootField) {
			SootField sf = (SootField)o;
			if(sf.isStatic()) {
				result = pta.reachingObjects(sf);
			}
			else {
				if(Properties.getPTFramework() == Properties.SPARK) {
					
					SootClass sc = sf.getDeclaringClass();
					FieldRefNode frn = ((PAG)pta).findGlobalFieldRefNode(loadedClasses.get(sc), sf);
					if(frn == null)
						frn = ((PAG)pta).findLocalFieldRefNode(loadedClasses.get(sc), sf);
					if(frn != null) {
						result = frn.makeP2Set();
					}
					
					
				}
			}
		}
		
		return result;
	}
	
	private HashMap<Object, LinkedList<Object>> objectSuccessorsMap = new HashMap<Object, LinkedList<Object>>();

	private LinkedList<Object> getSuccessors(Object o) {
		LinkedList<Object> successors = this.objectSuccessorsMap.get(o);
		if (successors == null) {
			successors = new LinkedList<Object>();
			// computes list of successors
			dfs(o, successors);
			this.objectSuccessorsMap.put(o, successors);
		}
		return successors;
	}
	
	/**
	 * computes the pointsToSet of o according to the current graph
	 * @param o
	 * @return
	 */
	public PointsToSet getPointsToSet(Object o) {

		// list of all successors for o, according to the current graph
		LinkedList<Object> successors = this.getSuccessors(o);
		
		// initial pointsToSet for o
		PointsToSet pts = this.getPts(o);
		if(pts == null || pts.isEmpty())
			return null;
				
		// merge o's pointsToSet with pointsToSets of its successors 
		for(Object succ : successors) {
			if(succ instanceof Local) {
				PointsToSet tmpPts = this.getAnalysisPTS(succ);
				if(tmpPts == null)
					continue;
				
				if(Properties.getPTFramework() == Properties.SPARK) {
					// make sure the pointsToSets have the same type
					if(((PointsToSetInternal)pts).getType() == null)
						((PointsToSetInternal)pts).setType(((PointsToSetInternal)tmpPts).getType());
					
					if(((PointsToSetInternal)tmpPts).getType() == null)
						((PointsToSetInternal)tmpPts).setType(((PointsToSetInternal)pts).getType());
					
					// merge
					((PointsToSetInternal)pts).mergeWith((PointsToSetInternal)tmpPts);
				}	
			}
		}
		
		return pts;
	}
		
	/**
	 * performs dfs on the current graph for o
	 * @param o
	 * @param successors - the successors of o (output parameter)
	 */
	private void dfs(Object o, LinkedList<Object> successors) {
		if(lists.containsKey(o)) {
			for(Object succ : lists.get(o)) {
				if(!successors.contains(succ)) {
					successors.add(succ);
					dfs(succ, successors);
				}
			}
		}
	}
	
}