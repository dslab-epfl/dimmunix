package dimmunix;

enum EventType {REQUEST, ACQUIRE, RELEASE, YIELD, GRANT, WAKE_UP};

public class Event {
	long time;
	EventType type = null;
	ThreadNode thread = null;
	LockNode lock = null;
	Position position = null;
	boolean posInHistory = false;

	public String toString() {
		return thread+ " "+ type+ " "+ lock+ " "+ position;
	}
}
