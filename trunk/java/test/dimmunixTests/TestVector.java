package dimmunixTests;



public class TestVector {
	
	public static void main(String[] args) {
		final MyVector<Object> v1 = new MyVector<Object>();
		final MyVector<Object> v2 = new MyVector<Object>();
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {		
				for (int i = 1; i <= 100000; i++)
					v1.addAll(v2);
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {		
				for (int i = 1; i <= 100000; i++)
					v2.addAll(v1);
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
