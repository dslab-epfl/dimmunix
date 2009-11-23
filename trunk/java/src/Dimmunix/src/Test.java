
public class Test {
	
	static void sleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
		}		
	}

	public static void main(String[] args) {
		final Object x1 = new Object(), x2 = new Object(), x3 = new Object();
		
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
