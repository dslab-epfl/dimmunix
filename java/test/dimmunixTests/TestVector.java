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
