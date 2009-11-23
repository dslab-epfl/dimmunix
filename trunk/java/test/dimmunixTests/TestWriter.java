package dimmunixTests;



import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TestWriter {

	public static void main(String[] args) {
		final CharArrayWriter cw = new CharArrayWriter();
		final PrintWriter pw1 = new PrintWriter(cw);
		final PrintWriter pw2 = new PrintWriter(pw1);

		Thread t1 = new Thread(new Runnable() {
			public void run() {
				pw2.write("x");
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				try {
					cw.writeTo(pw2);
				} catch (IOException e) {
				}
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
