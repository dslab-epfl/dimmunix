package dimmunixTests;

public class Test {
	
	static synchronized void sleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
		}		
	}
	
	synchronized void f(int x) {
		
	}

	public static void main(String[] args) {
		final Object x1 = new Object(), x2 = new Object(), x3 = new Object();
		sleep(10);
		
		new Test().f(10);
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				synchronized (x3) {
					sleep(100);
					synchronized (x1) {
						sleep(500);
						synchronized (x2) {
						}
					}					
				}
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				synchronized (x2) {
					sleep(500);
					synchronized (x1) {
						synchronized (x3) {							
						}
					}
				}
			}
		});
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
		}
		
		System.out.println("done");
	}
}
