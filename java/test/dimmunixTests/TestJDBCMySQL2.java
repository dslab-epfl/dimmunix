/*
     Created by Horatiu Jula, George Candea, Daniel Tralamazza, Cristian Zamfir
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix.

     Dimmunix is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

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
