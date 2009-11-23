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
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PerfTestThread extends Thread {
	
	void f0() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f1() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f2() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f3() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f4() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f5() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f6() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f7() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f8() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f9() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f10() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f11() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f12() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f13() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f14() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f15() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f16() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f17() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f18() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f19() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f20() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f21() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f22() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f23() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f24() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f25() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f26() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f27() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f28() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void f29() {
		if (depth == 1) {
			synchronized (TestPerformance.contention? sharedLocks[lIndex]: lock) {
				nSyncs++;
				sleep(syncDelayInside);        			
			}
			lIndex = rand.nextInt(sharedLocks.length);
			nextF();
			sleep(syncDelayOutside);
			return;
		}
		nextF();
		depth--;
		switch (fIndex) {
		case 0:
			f0();
			break;
		case 1:
			f1();
			break;
		case 2:
			f2();
			break;
		case 3:
			f3();
			break;
		case 4:
			f4();
			break;
		case 5:
			f5();
			break;
		case 6:
			f6();
			break;
		case 7:
			f7();
			break;
		case 8:
			f8();
			break;
		case 9:
			f9();
			break;
		case 10:
			f10();
			break;
		case 11:
			f11();
			break;
		case 12:
			f12();
			break;
		case 13:
			f13();
			break;
		case 14:
			f14();
			break;
		case 15:
			f15();
			break;
		case 16:
			f16();
			break;
		case 17:
			f17();
			break;
		case 18:
			f18();
			break;
		case 19:
			f19();
			break;
		case 20:
			f20();
			break;
		case 21:
			f21();
			break;
		case 22:
			f22();
			break;
		case 23:
			f23();
			break;
		case 24:
			f24();
			break;
		case 25:
			f25();
			break;
		case 26:
			f26();
			break;
		case 27:
			f27();
			break;
		case 28:
			f28();
			break;
		case 29:
			f29();
			break;
		}
	}

	void sleep(int delay) {
		long t0 = System.nanoTime();
		long target = t0+ 1000* delay;
		long t;
		while ((t=System.nanoTime()) < target && t < timeout);
		sumSleep += t- t0;
	}

	//timeout is in nanosec
	public PerfTestThread(CyclicBarrier barrier, int syncDelayInside, int syncDelayOutside) {
		this.barrier = barrier;
		this.syncDelayInside = syncDelayInside;
		this.syncDelayOutside = syncDelayOutside;		
	}
	
	void nextF() {
		int n = 2;
		fIndex = TestPerformance.randomization? ((fIndex+ 1+ rand.nextInt(n))% stackSize): ((fIndex+ 1)% stackSize);
	}

	CyclicBarrier barrier;
	int syncDelayInside, syncDelayOutside;
	int nSyncs = 0;
	static Object[] sharedLocks;
	long sumSleep = 0;
	volatile long timeout = 0;
	Random rand = new Random(this.getId());
	int fIndex, lIndex, depth;
	Object lock = new Object();
	int stackSize = 30;
	
	public void run() {
		try {
			barrier.await();
		} catch (InterruptedException e) {
		} catch (BrokenBarrierException e) {
		}
		
		lIndex = rand.nextInt(sharedLocks.length);
		fIndex = rand.nextInt(stackSize);
		
		try {
			while (timeout == 0 || System.nanoTime() < timeout) {
				depth = stackSize;            	
				switch (fIndex) {
				case 0:
					f0();
					break;
				case 1:
					f1();
					break;
				case 2:
					f2();
					break;
				case 3:
					f3();
					break;
				case 4:
					f4();
					break;
				case 5:
					f5();
					break;
				case 6:
					f6();
					break;
				case 7:
					f7();
					break;
				case 8:
					f8();
					break;
				case 9:
					f9();
					break;
				case 10:
					f10();
					break;
				case 11:
					f11();
					break;
				case 12:
					f12();
					break;
				case 13:
					f13();
					break;
				case 14:
					f14();
					break;
				case 15:
					f15();
					break;
				case 16:
					f16();
					break;
				case 17:
					f17();
					break;
				case 18:
					f18();
					break;
				case 19:
					f19();
					break;
				case 20:
					f20();
					break;
				case 21:
					f21();
					break;
				case 22:
					f22();
					break;
				case 23:
					f23();
					break;
				case 24:
					f24();
					break;
				case 25:
					f25();
					break;
				case 26:
					f26();
					break;
				case 27:
					f27();
					break;
				case 28:
					f28();
					break;
				case 29:
					f29();
					break;
				}
			}			
		}
		catch (RuntimeException e) {			
		}
	}
}
