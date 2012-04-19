package dimmunix;

import dimmunix.condvar.DimmunixCondVar;
import dimmunix.deadlock.DimmunixDeadlock;
import dimmunix.external.DimmunixExternal;
import dimmunix.hybrid.DimmunixHybrid;
import dimmunix.init.DimmunixInitDlcks;
import dimmunix.profiler.LockStatistics;
import dimmunix.profiler.NonMutexStats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.Semaphore;

public privileged aspect Instrumentation {

	//--------------------------SYNC BLOCKS-----------------------------------------	
	
	before(Object l): lock() && args(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.dimmunixEnabled) {
				DimmunixDeadlock.instance.avoidance(l, true);
			}
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.beforeLock(l);
			}
			if (Configuration.instance.initEnabled) {
				DimmunixInitDlcks.instance.beforeLock(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}
	}
	
	after(Object l): lock() && args(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.dimmunixEnabled) {
				DimmunixDeadlock.instance.acquire(l, true);
			}
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.afterLock(l);
			}				
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.afterLock(l);
			}
			if (Configuration.instance.initEnabled) {
				DimmunixInitDlcks.instance.afterLock(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}	
	
	before(Object l): unlock() && args(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.dimmunixEnabled) {
				DimmunixDeadlock.instance.release(l, true);
			}
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.beforeUnlock(l);
			}				
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.beforeUnlock(l);
			}
			if (Configuration.instance.initEnabled) {
				DimmunixInitDlcks.instance.beforeUnlock(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	before(): execution(synchronized * *.*(..)) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
	}
	
	//--------------------------COND VARS-----------------------------------------
	
/*	before(Object obj): call(void *.wait()) && target(obj) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.beforeWait(obj, 0);
			}				
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.beforeWait(obj);
				NonMutexStats.instance.addWaitInfo(obj);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}	
	
	after(Object obj) throws InterruptedException: call(void *.wait()) && target(obj) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.afterWait(obj, 0);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
		
	before(Object obj, long timeout): call(void *.wait(long)) && args(timeout) && target(obj) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.beforeWait(obj, timeout);
			}				
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.beforeWait(obj);
				NonMutexStats.instance.addWaitInfo(obj);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}	
	
	after(Object obj, long timeout) throws InterruptedException: call(void *.wait(long)) && args(timeout) && target(obj) && !within(communix.* || dimmunix.* || dimmunix.analysis.* || dimmunix.condvar.* || dimmunix.deadlock.* || dimmunix.profiler.*) {
		try {
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.afterWait(obj, timeout);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}	
		
	before(Object obj): call(void *.notify()) && target(obj) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.beforeNotify(obj);
			}				
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.beforeNotify(obj);
				NonMutexStats.instance.addNotifyInfo(obj);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	before(Object obj): call(void *.notifyAll()) && target(obj) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.condVarEnabled) {
				DimmunixCondVar.instance.beforeNotify(obj);
			}				
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.beforeNotify(obj);
				NonMutexStats.instance.addNotifyInfo(obj);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}	
	
	//--------------------------STATIC INIT-----------------------------------------
	
	after(): staticinitialization(tests.*) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		String cls = thisJoinPointStaticPart.getSignature().getDeclaringTypeName();
		DimmunixInitDlcks.instance.afterInit(cls);
	}
	
	//--------------------------REENTRANT LOCKS-----------------------------------------
	
	before(ReentrantLock l): call(void ReentrantLock.lock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.dimmunixEnabled) {
				DimmunixDeadlock.instance.avoidance(l, false);
			}
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeLock(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}		
	} 
	
	after(ReentrantLock l): call(void ReentrantLock.lock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.dimmunixEnabled) {
				DimmunixDeadlock.instance.acquire(l, false);
			}
			if (Configuration.instance.profilerEnabled) {
				LockStatistics.instance.afterLock(l);
			}
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.afterLock(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	} 
	
	before(ReentrantLock l): call(void ReentrantLock.unlock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.dimmunixEnabled) {
				DimmunixDeadlock.instance.release(l, false);
			}
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeUnlock(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	 
//--------------------------READ-WRITE LOCKS-----------------------------------------
 	
	before(ReentrantReadWriteLock.ReadLock l): call(void ReentrantReadWriteLock.ReadLock.lock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeLockr(l);
			}
			if (Configuration.instance.profilerEnabled) {
				NonMutexStats.instance.addRWLockInfo(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	after(ReentrantReadWriteLock.ReadLock l): call(void ReentrantReadWriteLock.ReadLock.lock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.afterLockr(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	before(ReentrantReadWriteLock.ReadLock l): call(void ReentrantReadWriteLock.ReadLock.unlock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeUnlockr(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	before(ReentrantReadWriteLock.WriteLock l): call(void ReentrantReadWriteLock.WriteLock.lock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeLockw(l);
			}
			if (Configuration.instance.profilerEnabled) {
				NonMutexStats.instance.addRWLockInfo(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	after(ReentrantReadWriteLock.WriteLock l): call(void ReentrantReadWriteLock.WriteLock.lock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.afterLockw(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	before(ReentrantReadWriteLock.WriteLock l): call(void ReentrantReadWriteLock.WriteLock.unlock()) && target(l) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeUnlockw(l);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
//--------------------------SEMAPHORES-----------------------------------------
	
	before(Semaphore s): call(void Semaphore.acquire()) && target(s) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeAcquire(s);
			}
			if (Configuration.instance.profilerEnabled) {
				NonMutexStats.instance.addSemaphoreInfo(s);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}

	after(Semaphore s): call(void Semaphore.acquire()) && target(s) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.afterAcquire(s);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}
	
	before(Semaphore s): call(void Semaphore.release()) && target(s) && !within(communix.* || dimmunix.* || dimmunix.*.*) {		
		try {
			if (Configuration.instance.hybridEnabled) {
				DimmunixHybrid.instance.beforeRelease(s);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}			
	}	
	
//--------------------------FILE LOCKS-----------------------------------------
	
	FileLock around(FileChannel f): call(FileLock FileChannel.lock()) && target(f) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		//before lock
		try {
			if (Configuration.instance.externalEnabled) {
				DimmunixExternal.instance.beforeLock(f);
			}
			if (Configuration.instance.profilerEnabled) {
				NonMutexStats.instance.addFileLockInfo(f);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}
		
		FileLock flock = proceed(f);
		
		//after lock
		try {
			if (Configuration.instance.externalEnabled) {
				DimmunixExternal.instance.afterLock(f);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}	
		
		return flock;
	}
	
	before(FileLock f): call(void FileLock.release()) && target(f) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		try {
			if (Configuration.instance.externalEnabled) {
				DimmunixExternal.instance.beforeUnlock(f);
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}					
	}
	
	FileInputStream around(File f): call(FileInputStream.new(File)) && args(f) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		FileInputStream fobj = proceed(f);
		DimmunixExternal.instance.updateFilePathMap(fobj, f.getAbsolutePath());
		return fobj;
	}
	
	FileInputStream around(String fname): call(FileInputStream.new(String)) && args(fname) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		FileInputStream fobj = proceed(fname);		
		DimmunixExternal.instance.updateFilePathMap(fobj, new File(fname).getAbsolutePath());
		return fobj;
	}
	
	FileOutputStream around(File f): call(FileOutputStream.new(File)) && args(f) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		FileOutputStream fobj = proceed(f);
		DimmunixExternal.instance.updateFilePathMap(fobj, f.getAbsolutePath());
		return fobj;
	}
	
	FileOutputStream around(String fname): call(FileOutputStream.new(String)) && args(fname) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		FileOutputStream fobj = proceed(fname);		
		DimmunixExternal.instance.updateFilePathMap(fobj, new File(fname).getAbsolutePath());
		return fobj;
	}
	
	RandomAccessFile around(File f, String mode): call(RandomAccessFile.new(File, String)) && args(f, mode) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		RandomAccessFile fobj = proceed(f, mode);		
		DimmunixExternal.instance.updateFilePathMap(fobj, f.getAbsolutePath());
		return fobj;
	}
	
	RandomAccessFile around(String fname, String mode): call(RandomAccessFile.new(String, String)) && args(fname, mode) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		RandomAccessFile fobj = proceed(fname, mode);		
		DimmunixExternal.instance.updateFilePathMap(fobj, new File(fname).getAbsolutePath());
		return fobj;
	}
	
	FileChannel around(Object fobj): call(FileChannel *.getChannel()) && target(fobj) && !within(communix.* || dimmunix.* || dimmunix.*.*) {
		FileChannel fch = null;
		if (fobj instanceof FileInputStream) {
			fch = ((FileInputStream)fobj).getChannel();
		}
		if (fobj instanceof FileOutputStream) {
			fch = ((FileOutputStream)fobj).getChannel();
		}
		if (fobj instanceof RandomAccessFile) {
			fch = ((RandomAccessFile)fobj).getChannel();
		}
		if (fobj != null) {
			DimmunixExternal.instance.updateFileObjMap(fch, fobj);
		}
		return fch;
	}*/
}

