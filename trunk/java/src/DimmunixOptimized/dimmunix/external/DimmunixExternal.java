package dimmunix.external;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.loadtime.ExecutionPositions;

import dimmunix.Pair;
import dimmunix.Util;
import dimmunix.Vector;

public class DimmunixExternal {
	private class LockInfo {
		StackTraceElement acqPos = null;		
	}
	
	private class ThreadInfo {
		public final int tid;
		private Vector<Pair<String,StackTraceElement>> reqs = new Vector<Pair<String,StackTraceElement>>();
		private HashSet<Pair<String,StackTraceElement>> acqs = new HashSet<Pair<String,StackTraceElement>>();
		
		void request(String f, StackTraceElement pos) {
			this.reqs.add(new Pair<String, StackTraceElement>(f, pos));
		}
		
		void acquire() {
			Pair<String, StackTraceElement> x = this.reqs.remove();
			this.acqs.add(x);
		}
		
		void release(String f, StackTraceElement pos) {
			this.acqs.remove(new Pair<String, StackTraceElement>(f, pos)); 
		}
		
		ThreadInfo(int tid) {
			this.tid = tid;
		}
	}
	private Vector<ThreadInfo> threads = new Vector<ThreadInfo>();
	
	public static final DimmunixExternal instance = new DimmunixExternal();
	
	private DimmunixExternal() {
		for (int i = 0; i < 5000; i++) {
			this.threads.add(new ThreadInfo(i));
		}
	}
	
	private boolean offlineDetection = true;
	
	private final String dbURL = "jdbc:mysql://localhost:3306/dimmunix";
	private Connection dbConnection = null;
	private PreparedStatement reqStmt;
	private PreparedStatement acqStmt;
	private PreparedStatement relStmt;	
	private PreparedStatement cleanupStmt;	
	private PreparedStatement dlckStmt;	
//	private PreparedStatement addAllowStmt;
//	private PreparedStatement removeAllowStmt;
//	private PreparedStatement avoidStmt;
	
	private ConcurrentHashMap<Object, String> filePaths = new ConcurrentHashMap<Object, String>();
	private ConcurrentHashMap<FileChannel, Object> fileObjects = new ConcurrentHashMap<FileChannel, Object>();
	
	private HashMap<Integer, LockInfo> locks = new HashMap<Integer, LockInfo>();
	
	public void init() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			this.dbConnection = DriverManager.getConnection(dbURL, "root", "dimmunix");
			
			this.reqStmt = this.dbConnection.prepareStatement("insert into Edges values (?, ?, ?, ?, ?)");
			this.acqStmt = this.dbConnection.prepareStatement("update Edges set state = 'hold' where tid = ? and pid = ? and file = ? and pos = ?");
			this.relStmt = this.dbConnection.prepareStatement("delete from Edges where tid = ? and pid = ? and state = 'hold' and file = ?");
			
			this.dlckStmt = this.dbConnection.prepareStatement("insert into History " +
					"select distinct e1.pos, e2.pos, e3.pos, e4.pos " +
					"from Edges e1, Edges e2, Edges e3, Edges e4 where " +
					"e1.state = 'hold' and e2.state = 'request' and e3.state = 'hold' and e4.state = 'request' and " +
					"e1.tid = e2.tid and e1.pid = e2.pid and e2.file = e3.file and " +
					"e3.tid = e4.tid and e3.pid = e4.pid and e4.file = e1.file and " +
					"e1.file != e3.file and (e1.tid != e3.tid or e1.pid != e3.pid)");
			
			this.cleanupStmt = this.dbConnection.prepareStatement("delete from Edges where pid = ?");
			
//			this.addAllowStmt = this.dbConnection.prepareStatement("insert into Edges values (?, ?, 'allow', ?, ?)");
//			this.removeAllowStmt = this.dbConnection.prepareStatement("delete from Edges where tid = ? and pid = ? and state = 'allow' and file = ?");
			
//			this.avoidStmt = this.dbConnection.prepareStatement("select * from Edges e1, Edges e2, History h where " +
//					"e1.state = 'allow' and e2.state = 'allow' and "+
//					"e1.tid = ? and e1.pid = ? and " +
//					"e1.file = ? and e1.pos = ? and " +
//					"(e1.tid != e2.tid or e1.pid != e2.pid) and e1.file != e2.file and " +
//					"e1.pos = h.Pout1 and e2.pos = h.Pout2");
			
			this.loadHistory();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private LockInfo getLockInfo(Object obj) {
		Integer id = System.identityHashCode(obj);
		LockInfo linfo = this.locks.get(id);
		if (linfo == null) {
			linfo = new LockInfo();
			this.locks.put(id, linfo);
		}
		return linfo;
	}
	
	private void loadHistory() {
		try {
			ResultSet res = this.dbConnection.createStatement().executeQuery("select Pout1, Pout2 from History");
			
			int i = 1;
			while (res.next()) {
				StackTraceElement pos1 = Util.parsePosition(res.getString(1));
				StackTraceElement pos2 = Util.parsePosition(res.getString(2));
				Signature sig = new Signature(i, pos1, pos2);
				History.instance.addSignature(sig);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void cleanup() {
		try {
			this.cleanupStmt.clearParameters();			
			this.cleanupStmt.setInt(1, Util.getPID());			
			this.cleanupStmt.execute();			
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
	
	public void shutDown() {
		if (this.offlineDetection) {
			this.checkForDeadlocksOffline();
		}
		
		try {
			this.cleanup();
			
			this.dbConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void requestToRAG(int tid, String fpath, StackTraceElement pos) throws SQLException {
		this.reqStmt.clearParameters();			
		this.reqStmt.setInt(1, tid);
		this.reqStmt.setInt(2, Util.getPID());
		this.reqStmt.setString(3, "request");
		this.reqStmt.setString(4, fpath);
		this.reqStmt.setString(5, pos.toString());			
		this.reqStmt.execute();	
	}
	
	private synchronized void acquireToRAG(int tid, String fpath, StackTraceElement pos) throws SQLException {
		this.acqStmt.clearParameters();			
		this.acqStmt.setInt(1, tid);
		this.acqStmt.setInt(2, Util.getPID());
		this.acqStmt.setString(3, fpath);
		this.acqStmt.setString(4, pos.toString());			
		this.acqStmt.execute();											
	}
	
	private synchronized void releaseToRAG(int tid, String fpath) throws SQLException {
		this.relStmt.clearParameters();			
		this.relStmt.setInt(1, tid);
		this.relStmt.setInt(2, Util.getPID());
		this.relStmt.setString(3, fpath);			
		this.relStmt.execute();									
	}
	
	public void beforeLock(FileChannel f) {
		Thread t = Thread.currentThread();
		StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId(), false);
		
		try {
			int tid = (int)t.getId();
			String fpath = this.getFilePath(f);
			
			if (this.offlineDetection) {
				this.threads.get(tid).request(fpath, pos);
			}
			else {
				this.requestToRAG(tid, fpath, pos);
				
				this.checkForDeadlocksSQL();					
			}
			
			for (Signature sig: History.instance.getSignatures()) {
				if (sig.contains(pos)) {
					sig.lock();						
				}
			}				
			
//			this.addAllowStmt.clearParameters();
//			this.addAllowStmt.setInt(1, tid);
//			this.addAllowStmt.setInt(2, pid);
//			this.addAllowStmt.setString(3, fpath);
//			this.addAllowStmt.setString(4, p);
//			this.addAllowStmt.execute();

//			instancesFound = this.checkInstantiations(tid, pid, fpath, p);

//			while (instancesFound) {
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				
//				instancesFound = this.checkInstantiations(tid, pid, fpath, p);
//			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}
	
	public void afterLock(FileChannel f) {
		Thread t = Thread.currentThread();				
		StackTraceElement pos = ExecutionPositions.instance.getCurrentPosition(t.getId());

		try {
			int tid = (int)t.getId();
			String fpath = this.getFilePath(f);
			
			synchronized (this) {
				getLockInfo(f).acqPos = pos;//for avoidance				
			}			
			
			if (this.offlineDetection) {
				this.threads.get(tid).acquire();
			}
			else {
				this.acquireToRAG(tid, fpath, pos);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 	
	}
	
	public void beforeUnlock(FileLock f) {
		Thread t = Thread.currentThread();
		FileChannel fch = f.channel();

		try {
			int tid = (int)t.getId();
			String fpath = this.getFilePath(fch);

			StackTraceElement acqPos;
			synchronized (this) {
				acqPos = this.getLockInfo(fch).acqPos;				
			}
			
			if (this.offlineDetection) {
				this.threads.get(tid).release(fpath, acqPos);
			}
			else {
				this.releaseToRAG(tid, fpath);
			}
			
			for (Signature sig: History.instance.getSignatures()) {
				if (sig.contains(acqPos)) {
					sig.unlock();						
				}
			}

//			this.removeAllowStmt.clearParameters();
//			this.removeAllowStmt.setInt(1, tid);
//			this.removeAllowStmt.setInt(2, pid);
//			this.removeAllowStmt.setString(3, fpath);
//			this.removeAllowStmt.execute();			
		} catch (SQLException e) {
			e.printStackTrace();
		} 			
	}
	
	private void checkForDeadlocksSQL() {
		try {
			this.dlckStmt.execute();
			int res = this.dlckStmt.getUpdateCount();
			if (res > 0) {				
				System.out.println((res/ 2)+ " external deadlocks found !");
			}				
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void checkForDeadlocksOffline() {
		try {
			for (ThreadInfo tinfo: this.threads) {
				for (Pair<String, StackTraceElement> req: tinfo.reqs) {
					this.requestToRAG(tinfo.tid, req.v1, req.v2);
				}
				for (Pair<String, StackTraceElement> acq: tinfo.acqs) {
					this.acquireToRAG(tinfo.tid, acq.v1, acq.v2);
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		this.checkForDeadlocksSQL();
	}
	
	public void updateFilePathMap(Object fileObj, String path) {
		this.filePaths.put(fileObj, path);
	}
	
	public void updateFileObjMap(FileChannel fch, Object fobj) {
		this.fileObjects.put(fch, fobj);
	}
	
	private String getFilePath(FileChannel f) {
		Object fobj = this.fileObjects.get(f);
		if (fobj == null) {
			return "null";
		}
		String path = this.filePaths.get(fobj);
		if (path == null) {
			return "null";
		}
		return path;
	}
	
/*	private boolean checkInstantiations(int tid, int pid, String f, String p) {
		try {
//			this.avoidStmt.clearParameters();
//			this.avoidStmt.setInt(1, tid);
//			this.avoidStmt.setInt(2, pid);
//			this.avoidStmt.setString(3, f);
//			this.avoidStmt.setString(4, p);
//			ResultSet res = this.avoidStmt.executeQuery();
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ResultSet res = this.dbConnection.createStatement().executeQuery("select * from Edges e1, Edges e2, History h where " +
					"e1.state = 'allow' and e2.state = 'allow' and "+
					"(e1.tid != e2.tid or e1.pid != e2.pid) and e1.file != e2.file and " +
					"e1.pos = h.Pout1 and e2.pos = h.Pout2");
			
			return res.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}*/
}
