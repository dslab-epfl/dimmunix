package dimmunixTests;




public class TestStringBuffer {
	
	public static void main(String[] args) {
		final StringBuffer sb1 = new StringBuffer();
		final StringBuffer sb2 = new StringBuffer();
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				for (int i = 1; i <= 100000; i++)
					sb1.append(sb2);
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {				
				for (int i = 1; i <= 100000; i++)
					sb2.append(sb1);
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
