/*
 *  test_perf.h
 *
 *
 *  Created by Daniel Tralamazza on 4/15/08.
 *  Copyright 2008. All rights reserved.
 *
 */

#ifndef __TEST_PERF_H
#define __TEST_PERF_H


#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>

#include <sys/time.h>
#include <time.h>
#include <pthread.h>
#include <stdlib.h>

using namespace std;

class PerfThread {
public:
	PerfThread(int _idx, int _lock_idx, long _timeout = 0)
		: func_idx(_idx % 10), lock_idx(_lock_idx), timeout(_timeout), synch_count(0), running(false), seed(rand()) { /*rand_seq.reserve(1000000);*/ }

	bool start() { return pthread_create(&tid, 0, PerfThread::thr_run, this) == 0; }

	int wait() {
		pthread_join(tid, 0);

/*		ofstream of;
		stringstream st;
		st << "test_perf.r" << tid;
		of.open(st.str().c_str());
		for (vector<int>::iterator it = rand_seq.begin(); it != rand_seq.end(); ++it)
			of << *it << endl;
		of.close();*/

		return synch_count;
	}

	void cancel() {
		if (running) {
			running = false;
			pthread_cancel(tid);
		}
	}

private:
	int func_idx;
	int lock_idx;
	long timeout;
	unsigned seed;

//	vector<int> rand_seq;

	volatile bool running;
	int synch_count;
	int depth;
	pthread_t tid;
	void jump();
	static void* thr_run(void* arg);

	void f0(); void f1(); void f2(); void f3(); void f4(); void f5(); void f6(); void f7(); void f8(); void f9();
};


#endif
