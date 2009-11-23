package dimmunixTests;



import java.util.Hashtable;

public class MyHashTable<K, V> extends Hashtable<K, V> {

	private static final long serialVersionUID = 4577504604507512932L;

	@Override
	public synchronized boolean equals(Object o) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return super.equals(o);
	}

	@Override
	public synchronized int size() {
		return super.size();
	}	
}
