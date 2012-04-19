package dimmunix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;

import org.aspectj.weaver.loadtime.ExecutionPositions;
import org.aspectj.weaver.loadtime.UnchangedClasses;

import soot.G;
import soot.Scene;
import soot.options.Options;

import communix.SignatureValidator;

import dimmunix.analysis.Analysis;
import dimmunix.analysis.CondVarAnalysis;
import dimmunix.analysis.HashAnalysis;
import dimmunix.analysis.NestedSyncBlockAnalysis;
import dimmunix.analysis.SkipAvoidanceAnalysis;
import dimmunix.condvar.DimmunixCondVar;
import dimmunix.deadlock.DimmunixDeadlock;
import dimmunix.external.DimmunixExternal;
import dimmunix.hybrid.DimmunixHybrid;
import dimmunix.init.DimmunixInitDlcks;
import dimmunix.init.StaticInitAnalysis;
import dimmunix.profiler.LockStatistics;
import dimmunix.profiler.NonMutexStats;

public class DimmunixAgent {

	private static void loadClassPath() {
		String clsPath = "java.class.path";
		String classPath = System.getProperty(clsPath);

		try {
			BufferedReader br = new BufferedReader(new FileReader("classpath"));
			String cl;			
			while ((cl = br.readLine()) != null) {
				classPath += File.pathSeparator + cl;
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.setProperty(clsPath, classPath);
	}
	
	private static void init() {
		loadClassPath();
		// Options.v().set_whole_program(true);
		// Options.v().set_via_shimple(true);
		Options.v().set_keep_line_number(true);
		// Options.v().set_keep_offset(true);
		// Options.v().set_src_prec(Options.src_prec_only_class);
		// Options.v().set_app(false);
		// Options.v().set_validate(true);
		// Options.v().set_output_format(Options.output_format_class);
		Options.v().set_soot_classpath(System.getProperty("java.class.path"));
		Options.v().set_allow_phantom_refs(true);
		
		G.v().out = new PrintStream(new OutputStream() {
	        public void close() {}
	        public void flush() {}
	        public void write(byte[] b) {}
	        public void write(byte[] b, int off, int len) {}
	        public void write(int b) {}
	    });


		Scene.v().loadBasicClasses();

		Configuration.instance.init();
		
		UnchangedClasses.load();
		
		ExecutionPositions.instance.init();
		
		if (Configuration.instance.dimmunixEnabled) {
			DimmunixDeadlock.instance.init();
			Analysis.instance.init();
			SkipAvoidanceAnalysis.instance.init();
			
			if (Configuration.instance.communixEnabled) {
				NestedSyncBlockAnalysis.instance.init();
				HashAnalysis.instance.init();
				
				SignatureValidator.instance.run();
			}
		}
		if (Configuration.instance.condVarEnabled) {
			DimmunixCondVar.instance.init();
			Analysis.instance.init();
			CondVarAnalysis.instance.init();
		}
		if (Configuration.instance.initEnabled) {
			DimmunixInitDlcks.instance.init();
			Analysis.instance.init();
			StaticInitAnalysis.instance.init();
		}
		if (Configuration.instance.hybridEnabled) {
			DimmunixHybrid.instance.init();
		}
		
		if (Configuration.instance.externalEnabled) {
			DimmunixExternal.instance.init();
		}
	}
	
	private static void shutDown() {
		if (Configuration.instance.profilerEnabled) {
			LockStatistics.instance.printStatistics();
			NonMutexStats.instance.collectStats();
		}
		
/*		if (Configuration.instance.dimmunixEnabled) {
			DimmunixDeadlock.instance.shutDown();
			
			Analysis.instance.checkIfThereAreNewLoadedClasses();
			if (Analysis.instance.newLoadedClasses) {
				Analysis.instance.saveLoadedClasses();
			}
			
			boolean doSkipAvoidanceAnaylsis = DimmunixDeadlock.instance.history.size() > 0 && (Analysis.instance.newLoadedClasses || DimmunixDeadlock.instance.newCyclesFound);
			boolean doCommunixAnalysis = Configuration.instance.communixEnabled && Analysis.instance.newLoadedClasses;
			
			if (doCommunixAnalysis || doSkipAvoidanceAnaylsis) {
				try {
					Analysis.instance.run();

					if (doCommunixAnalysis) {
						NestedSyncBlockAnalysis.instance.run();
						NestedSyncBlockAnalysis.instance.saveResults();
						
						HashAnalysis.instance.saveResults();
					}
					
					if (doSkipAvoidanceAnaylsis) {
						SkipAvoidanceAnalysis.instance.run();
						SkipAvoidanceAnalysis.instance.saveResults();								
					}
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
		}
		
		if (Configuration.instance.condVarEnabled) {
			Analysis.instance.checkIfThereAreNewLoadedClasses();
			if (Analysis.instance.newLoadedClasses) {
				Analysis.instance.saveLoadedClasses();
				
				Analysis.instance.run();

				CondVarAnalysis.instance.run();				
				CondVarAnalysis.instance.saveResults();
			}
			
			DimmunixCondVar.instance.shutDown();			
		}
		
		if (Configuration.instance.initEnabled) {
			Analysis.instance.checkIfThereAreNewLoadedClasses();
			if (Analysis.instance.newLoadedClasses) {
				Analysis.instance.saveLoadedClasses();
				
				Analysis.instance.run();
				
				StaticInitAnalysis.instance.run();
				StaticInitAnalysis.instance.saveResults();
			}
			
			DimmunixInitDlcks.instance.shutDown();
		}
		
		if (Configuration.instance.hybridEnabled) {
			DimmunixHybrid.instance.shutDown();
		}
		
		if (Configuration.instance.externalEnabled) {
			DimmunixExternal.instance.shutDown();
		}*/
		
		UnchangedClasses.save();
		
		ExecutionPositions.instance.save();
	}

	public static void premain(String agentArguments, Instrumentation instrumentation) {
		init();

		ClassChecker clChecker = new ClassChecker();
		ClassTransformer clTransASM = new ClassTransformer();
		instrumentation.addTransformer(clChecker);
		instrumentation.addTransformer(clTransASM);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutDown();				
			}
		});
	}
}
