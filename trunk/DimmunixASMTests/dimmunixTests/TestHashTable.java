package dimmunixTests;

import java.util.Hashtable;

public class TestHashTable {

	public static void main(String[] args) {
		final Hashtable<String, Hashtable> h1 = new Hashtable<String, Hashtable>();
		final Hashtable<String, Hashtable> h2 = new Hashtable<String, Hashtable>();
		
		h1.put("2", h2);
		h2.put("1", h1);
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < 10000; i++)
					h1.equals(h2);
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < 10000; i++)
					h2.equals(h1);
			}
		});
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("done");		
	}
}
