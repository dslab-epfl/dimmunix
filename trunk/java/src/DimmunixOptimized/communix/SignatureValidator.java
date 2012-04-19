
package communix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import dimmunix.Configuration;
import dimmunix.analysis.Analysis;
import dimmunix.analysis.HashAnalysis;
import dimmunix.analysis.NestedSyncBlockAnalysis;
import dimmunix.deadlock.DimmunixDeadlock;

/*
 * this class is supposed to check the hashes and 
 * check if (1) all the top frames are sync stmts and (2) the top frames of the outer call stacks are nested sync stmts.
 * For efficiency, all the data needed for the checks is precomputed, so the filters should be fast; 
 * moreover, each signature is validated only once. 
*/
public class SignatureValidator {
	
	private final HashSet<Integer> validatedSignatures = new HashSet<Integer>();
	
	private final String dbFile;
	
	private final ThreadPoolExecutor threadPool;
	
	private SignatureValidator() {		
		this.dbFile = System.getProperty("user.home")+ "/client_Dimmunix.hist";
		
		this.threadPool = new ThreadPoolExecutor(16, 32, 10, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000000));		
		this.threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				r.run();
			}
		});
	}
	
	final AtomicInteger nAccepted = new AtomicInteger(0);
	final AtomicInteger nRejectedHash = new AtomicInteger(0);
	final AtomicInteger nRejectedCFG = new AtomicInteger(0);
	final AtomicInteger nMerged = new AtomicInteger(0);
	
	public static final SignatureValidator instance = new SignatureValidator();
	
	private void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("validated_signatures"));
			String line;
			while ((line = br.readLine()) != null) {
				this.validatedSignatures.add(Integer.parseInt(line));
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveResults() {
		try {
			PrintWriter pw = new PrintWriter("validated_signatures");
			for (Integer id: this.validatedSignatures) {
				pw.write(id + "\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void validate(Signature sig, int id) throws Exception {
		Signature sigHashFilter = this.checkHashes(sig, id);	
		if (sigHashFilter == null) {
			nRejectedHash.getAndIncrement();
			return;
		}
		boolean isValid = this.checkValidTopFrames(sigHashFilter, id);
		if (isValid) {
			dimmunix.deadlock.Signature sigDimm = DimmunixDeadlock.instance.toDimmunixSignature(sigHashFilter);
			if (!DimmunixDeadlock.instance.history.merge(sigDimm)) {
				DimmunixDeadlock.instance.history.add(sigDimm);
				nAccepted.getAndIncrement();
			}
			else {
				nMerged.getAndIncrement();
			}
		}
		else {
			nRejectedCFG.getAndIncrement();
		}		
	}
	
	public void run() {
		System.out.println("communix validating signatures");
		this.init();
		
		try {
			BufferedReader brSize = new BufferedReader(new FileReader(this.dbFile+ "_size"));
			int dbSize = Integer.parseInt(brSize.readLine());
			brSize.close();
			
			if (dbSize == this.validatedSignatures.size()) {
				System.out.println("there are no signatures to validate");
				return;
			}
			
			BufferedReader br = new BufferedReader(new FileReader(this.dbFile));
			String line;
			int id = 0;
			while ((line = br.readLine()) != null) {
				if (this.validatedSignatures.contains(id)) {
					continue;
				}
				
				try {
					final Signature sig = new Signature(line);
					final int sigId = id;
					this.threadPool.execute(new Runnable() {
						public void run() {
							try {
								SignatureValidator.this.validate(sig, sigId);
								synchronized (SignatureValidator.this.validatedSignatures) {
									SignatureValidator.this.validatedSignatures.add(sigId);									
								}
							} catch (Exception e) {
								e.printStackTrace();
							}							
						}
					});
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				
				id++;
			}
			br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.threadPool.shutdown();
		try {
			this.threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println(nAccepted+ " signatures accepted");
		System.out.println(nRejectedHash+ " signatures rejected by the hash filter");
		System.out.println(nRejectedCFG+ " signatures rejected by the CFG filter");
		System.out.println(nMerged+ " signatures merged into exiting signatures");							
		
		this.saveResults();
	}
	
	//if there is a top frame whose hash does not match, return null
	//otherwise, return the sig with the longest hash-matching suffixes
	private Signature checkHashes(Signature sig, int id) throws Exception {
		Vector<CallStack> outerStacksCommon = new Vector<CallStack>();
		Vector<CallStack> innerStacksCommon = new Vector<CallStack>();
		
		for (int i = 0; i < sig.size; i++) {
			CallStack csOuter = sig.outerStacks.get(i);
			CallStack csInner = sig.innerStacks.get(i);
			
			Vector<Frame> csOuterCommon = new Vector<Frame>();
			Vector<Frame> csInnerCommon = new Vector<Frame>();
			
			for (int j = 0; j < csOuter.frames.size(); j++) {
				Frame f = csOuter.frames.get(j);				
				String cl = f.frame.getClassName();
				long myHash = HashAnalysis.instance.getHash(cl);
				
				if (Configuration.instance.skip(cl)) {
					continue;
				}
				
				if (myHash == 0) {
					throw new Exception("not enough information to check hashes of signature "+ id);
				}
				
				if (j == 0 && myHash != f.hash) {
					return null;
				}
				
				if (myHash == f.hash) {
					csOuterCommon.add(f);
				}
				else {
					break;
				}
			}
			
			for (int j = 0; j < csInner.frames.size(); j++) {
				Frame f = csInner.frames.get(j);				
				String cl = f.frame.getClassName();
				long myHash = HashAnalysis.instance.getHash(cl);
				
				if (Configuration.instance.skip(cl)) {
					continue;
				}
				
				if (myHash == 0) {
					throw new Exception("not enough information to check hashes of signature "+ id);
				}
				
				if (j == 0 && myHash != f.hash) {
					return null;
				}
				
				if (myHash == f.hash) {
					csInnerCommon.add(f);
				}
				else {
					break;
				}
			}
			
			outerStacksCommon.add(new CallStack(csOuterCommon));
			innerStacksCommon.add(new CallStack(csInnerCommon));
		}
		
		return new Signature(outerStacksCommon, innerStacksCommon);
	}
	
	//check whether the top frames are nested sync blocks/methods
	private boolean checkValidTopFrames(Signature sig, int id) throws Exception {
		for (CallStack csOut: sig.outerStacks) {
			Frame fTop = csOut.frames.get(0);
			//if the class was loaded in the previous run, it means it was analyzed, so we can check stuff
			//otherwise, throw an Exception
			if (!Analysis.instance.loadedClassesPreviousRun.contains(fTop.frame.getClassName())) {
				throw new Exception("not enough information to check validity of signature "+ id);
			}
			if (!NestedSyncBlockAnalysis.instance.nestedSyncBlockPositions.contains(fTop.frame)) {
				return false;
			}
		}
		return true;
	}
}
