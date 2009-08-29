package dimmunix;

public class Yield {
	ThreadNode thread;
	LockNode lock;
	Position position;
	long time;
	
	public Yield(ThreadNode thread, LockNode lock, Position position, long time) {
		this.thread = thread;
		this.lock = lock;
		this.position = position;
		this.time = time;
	}	
}
