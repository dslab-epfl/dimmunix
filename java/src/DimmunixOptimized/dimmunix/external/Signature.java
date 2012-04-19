package dimmunix.external;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class Signature {
	public final int id;
	private RandomAccessFile avoidanceFileLock;
	public final StackTraceElement pos1; 
	public final StackTraceElement pos2;
	private FileLock avoidanceLock;
	private int nAcq = 0;
		
	public Signature(int id, StackTraceElement pos1, StackTraceElement pos2) {
		this.id = id;
		try {
			this.avoidanceFileLock = new RandomAccessFile(System.getProperty("user.home")+ "/avoidance_lock_"+ id, "rw");
		} catch (FileNotFoundException e) {
			this.avoidanceFileLock = null;
			e.printStackTrace();
		}
		this.pos1 = pos1;
		this.pos2 = pos2;
	}
	
	public synchronized void lock() {
		if (nAcq == 0) {
			try {
				this.avoidanceLock = this.avoidanceFileLock.getChannel().lock();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.nAcq++;
	}
	
	public synchronized void unlock() {
		this.nAcq--;
		if (this.nAcq == 0) {
			try {
				this.avoidanceLock.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void finalize() {
		try {
			this.avoidanceFileLock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean contains(StackTraceElement pos) {
		return this.pos1.equals(pos) || this.pos2.equals(pos);
	}
}
