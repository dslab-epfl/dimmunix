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
