package dIV.core.staticAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import soot.EntryPoints;
import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.Stmt;
import soot.jimple.spark.SparkTransformer;
import soot.options.Options;
import soot.util.Chain;
import dIV.util.Properties;

/**
 * Class that uses Spark Framework from soot to perform pointsTo analysis
 * 
 * @author cristina (cristina.basescu@gmail.com)
 * 
 */
public class PointsToAnalysis {

	// classes loaded in soot
	static HashMap<SootClass, Value> loadedClasses;
	// id for analyzed values
	static int counter = 0;
	static PtrGraph ptrGraph;

	/**
	 * loads a class in Soot's scene
	 * 
	 * @param name
	 *            - the name of the class to be loaded
	 * @param main
	 *            - says if the class is a main class
	 * @return
	 */
	private static SootClass loadClass(String name, boolean main) {
		SootClass c = Scene.v().loadClassAndSupport(name);
		c.setApplicationClass();
		c.setInScene(true);
		if (main)
			Scene.v().setMainClass(c);
		return c;
	}

	/**
	 * loads the specified classes so as to initialize the soot scene, then performs spark
	 * analysis
	 * 
	 * @param classesToBeLoaded
	 * @param mainClass
	 */
	public static void init(HashSet<String> classesToBeLoaded, String mainClass) {
		// load classes
		loadedClasses = new HashMap<SootClass, Value>();
		for (String classname : classesToBeLoaded)
			loadedClasses.put(loadClass(classname, false), null);

		// load main class
		//loadedClasses.put(loadClass(mainClass, true), null);

		// settings
		soot.Scene.v().loadNecessaryClasses();
		soot.Scene.v().setEntryPoints(EntryPoints.v().all());

		// analyze
		//if (Properties.getPTFramework() == Properties.SPARK)
			setSparkPointsToAnalysis();

		// set ptr graph
		ptrGraph = new PtrGraph(loadedClasses, Scene.v().getPointsToAnalysis());
	}

	/**
	 * sets Soot Framework
	 */
	private static void setSparkPointsToAnalysis() {
		System.out.println("[spark] Starting analysis ...");

		HashMap<String, String> opt = new HashMap<String, String>();

		opt.put("enabled", "true");
		opt.put("verbose", "true");
		opt.put("safe-newinstance", "true");
		opt.put("ignore-types", "false");
		opt.put("force-gc", "false");
		opt.put("pre-jimplify", "false");
		opt.put("vta", "false");
		opt.put("rta", "false");
		opt.put("field-based", "false");
		opt.put("types-for-sites", "false");
		opt.put("merge-stringbuffer", "true");
		opt.put("string-constants", "false");
		opt.put("simulate-natives", "true");
		opt.put("simple-edges-bidirectional", "false");
		opt.put("on-fly-cg", "true");
		opt.put("simplify-offline", "true");
		opt.put("simplify-sccs", "true");
		opt.put("ignore-types-for-sccs", "false");
		opt.put("propagator", "worklist");
		opt.put("set-impl", "double");
		opt.put("double-set-old", "hybrid");
		opt.put("double-set-new", "hybrid");
		opt.put("dump-html", "false");
		opt.put("dump-pag", "false");
		opt.put("dump-solution", "false");
		opt.put("topo-sort", "false");
		opt.put("dump-types", "true");
		opt.put("class-method-var", "true");
		opt.put("dump-answer", "false");
		opt.put("add-tags", "false");
		opt.put("set-mass", "false");

		SparkTransformer.v().transform("", opt);

		System.out.println("[spark] Done!");
	}

	private static HashMap<String, Map<Integer, Object>> methodValuesMap = new HashMap<String, Map<Integer, Object>>();

	private static Map<Integer, Object> getValuesFromMap(SootClass c, String m) {
		Map<Integer, Object> values = methodValuesMap.get(m);
		if (values == null) {
			values = getValues(c, m);
			methodValuesMap.put(m, values);
		}
		return values;
	}

	/**
	 * check whether value1 used in method1 from class1 may alias value2 used in method2
	 * from class2
	 * 
	 * @param class1
	 * @param method1
	 * @param value1
	 * @param class2
	 * @param method2
	 * @param value2
	 * @param type
	 * @return
	 * @throws PTAException
	 */
	public static boolean analyze(SootClass class1, String method1, Value value1,
			SootClass class2, String method2, Value value2) throws PTAException {

		// map that contains the variables to be analyzed and their ids
		Map<Integer, Object> ls = new HashMap<Integer, Object>();

		/*
		 * if given parameter classes are not in loadedClasses, they should be added
		 * because they have already been loaded
		 */
		if (!loadedClasses.containsKey(class1))
			loadedClasses.put(class1, null);
		if (!loadedClasses.containsKey(class2))
			loadedClasses.put(class2, null);

		Map<Integer, Object> auxLs1 = getValuesFromMap(class1, method1);
		ls.putAll(auxLs1);

		Map<Integer, Object> auxLs2 = getValuesFromMap(class2, method2);
		// add non-duplicates
		Set<Integer> keys = auxLs2.keySet();
		Iterator<Integer> keyIter = keys.iterator();
		while (keyIter.hasNext()) {
			Integer key = keyIter.next();
			Object value = auxLs2.get(key);
			if (!ls.containsValue(value))
				ls.put(key, value);
		}

		ptrGraph.setLoadedClasses(loadedClasses);

		return intersects(ls, value1, value2);
	}

	/**
	 * prints a map
	 * 
	 * @param ls
	 */
	private static void printMap(Map<Integer, Object> ls) {
		Set<Integer> keys = ls.keySet();
		Iterator<Integer> keyIter = keys.iterator();
		// System.out.println("The map contains the following associations:");
		while (keyIter.hasNext()) {
			Object key = keyIter.next();
			Object value = ls.get(key);
			// System.out.print( "   (" + key + "," + value + ") " );
			// if(value instanceof Value)
			// System.out.print("value");
			// System.out.println();
		}
	}

	/**
	 * finds all the values subject to alias analysis (field from class sc and local
	 * variables from methodname)
	 * 
	 * @param sc
	 *            - the analyzed class
	 * @param methodname
	 *            - the analyzed method from sc
	 * @param typename
	 *            - type of analyzed values
	 * @return a map with values and ids for them
	 */
	private static Map<Integer, Object> getValues(SootClass sc, String methodname) {

		Map<Integer, Object> res = new HashMap<Integer, Object>();

		// get sc's fields
		Chain<SootField> sfChain = sc.getFields();
		for (java.util.Iterator<SootField> it = sfChain.iterator(); it.hasNext();) {
			SootField sf = it.next();
			// assign numbers to fields
			Scene.v().getFieldNumberer().add(sf);
			if (!res.containsValue(sf))
				res.put(counter++, sf);

		}

		// analyze the method
		Iterator<SootMethod> mi = sc.getMethods().iterator();
		while (mi.hasNext()) {
			SootMethod sm = mi.next();
			if (true && sm.getName().equals(methodname) && sm.isConcrete()) {
				Iterator<Unit> ui = sm.retrieveActiveBody().getUnits().iterator();

				// analyze each statement in the method
				while (ui.hasNext()) {
					Stmt s = (Stmt) ui.next();

					// for definition statements, add edges to ptrGraph
					if (s instanceof DefinitionStmt) {
						DefinitionStmt ds = (DefinitionStmt) s;
						Value left = ds.getLeftOp();
						Value right = ds.getRightOp();
						if (left instanceof FieldRef) {
							SootField sf1 = ((FieldRef) left).getField();
							if (right instanceof FieldRef) {
								SootField sf2 = ((FieldRef) right).getField();
								ptrGraph.addEdge(sf1, sf2);
							}
							if (right instanceof Local) {
								Local l2 = (Local) right;
								ptrGraph.addEdge(sf1, l2);
							}
						}
						if (left instanceof Local) {
							Local l1 = (Local) left;
							if (right instanceof FieldRef) {
								SootField sf2 = ((FieldRef) right).getField();
								ptrGraph.addEdge(l1, sf2);
							}
							if (right instanceof Local) {
								Local l2 = (Local) right;
								ptrGraph.addEdge(l1, l2);
							}
						}
					}

					// find def boxes for the current statement and get values
					Iterator<ValueBox> bi = s.getDefBoxes().iterator();
					while (bi.hasNext()) {
						Object o = bi.next();
						if (o instanceof ValueBox) {
							Value v = ((ValueBox) o).getValue();

							// find base value for class sc
							if (s.toString().indexOf("@this") != -1) {
								loadedClasses.remove(sc);
								loadedClasses.put(sc, v);
							}

							if (!res.containsValue(v))
								res.put(counter++, v);
						}
					}
				}
			}
		}

		return res;
	}

	/**
	 * prints alias intersects from the given map
	 * 
	 * @param ls
	 */
	private static void print(Map<Integer, Object> ls) {
		Iterator<Map.Entry<Integer, Object>> i1 = ls.entrySet().iterator();
		while (i1.hasNext()) {
			Map.Entry<Integer, Object> e1 = i1.next();
			int p1 = e1.getKey().intValue();
			Object o1 = e1.getValue();
			PointsToSet r1 = ptrGraph.getPointsToSet(o1);
			if (r1 == null)
				continue;
			Iterator<Map.Entry<Integer, Object>> i2 = ls.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry<Integer, Object> e2 = i2.next();
				int p2 = e2.getKey().intValue();
				Object o2 = e2.getValue();
				PointsToSet r2 = ptrGraph.getPointsToSet(o2);
				if (r2 == null)
					continue;
				// if (p1 <= p2)
				// System.out.println("["+p1+","+p2+"]\t Container intersect? "+r1.hasNonEmptyIntersection(r2));
			}
		}
	}

	/**
	 * checks whether values v1 and v2 may alias
	 * 
	 * @param ls
	 * @param v1
	 * @param v2
	 * @return
	 */
	private static boolean intersects(Map<Integer, Object> ls, Value v1, Value v2) {

		// v1 and v2 are values subject to alias analysis, according to getValues method
		boolean found1 = false, found2 = false;

		if (Properties.getPrintInfo()) {
			printMap(ls);
			print(ls);
		}

		Iterator<Map.Entry<Integer, Object>> i1 = ls.entrySet().iterator();
		while (i1.hasNext()) {
			Map.Entry<Integer, Object> e1 = i1.next();
			int p1 = ((Integer) e1.getKey()).intValue();
			Object o1 = (Object) e1.getValue();

			if (!found1)
				if (o1 instanceof Value)
					if (((Value) o1).equals(v1))
						found1 = true;
			if (!found2)
				if (o1 instanceof Value)
					if (((Value) o1).equals(v2))
						found2 = true;

			PointsToSet r1 = ptrGraph.getPointsToSet(o1);
			if (r1 == null)
				continue;
			Iterator<Map.Entry<Integer, Object>> i2 = ls.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry<Integer, Object> e2 = i2.next();
				int p2 = ((Integer) e2.getKey()).intValue();
				Object o2 = (Object) e2.getValue();

				if (!found1)
					if (o2 instanceof Value)
						if (((Value) o2).equals(v1))
							found1 = true;
				if (!found2)
					if (o2 instanceof Value)
						if (((Value) o2).equals(v2))
							found2 = true;

				PointsToSet r2 = ptrGraph.getPointsToSet(o2);
				if (r2 == null)
					continue;
				if (p1 <= p2 && o1 instanceof Value && o2 instanceof Value) {
					if ((((Value) o1).equals(v1) && ((Value) o2).equals(v2))
							|| (((Value) o1).equals(v2) && ((Value) o2).equals(v1))) {
						return r1.hasNonEmptyIntersection(r2);
					}
				}
			}
		}

		// if one of the values isn't subject to alias, return negative result
		if (!found1 || !found2) {
			return false;
		}

		// conservative result
		return true;
	}
}
