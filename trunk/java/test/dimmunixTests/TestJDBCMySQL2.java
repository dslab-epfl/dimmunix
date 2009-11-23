package dimmunixTests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TestJDBCMySQL2 {
	
	public static void main(String[] args) {
		String conUrl = "jdbc:mysql://localhost:3306/test";
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			final Connection con = DriverManager.getConnection(conUrl, "horatiu", "horatiu");
			final Statement stm = con.prepareStatement("show tables");
				
			Thread t1 = new Thread(new Runnable() {
				public void run() {
					try {
						con.prepareStatement("show tables");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			Thread t2 = new Thread(new Runnable() {
				public void run() {
					try {
						stm.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			t1.start();
			t2.start();
			
			t1.join();
			t2.join();
			
			con.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
