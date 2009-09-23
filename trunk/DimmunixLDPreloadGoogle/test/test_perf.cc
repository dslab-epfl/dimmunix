/*
 *  test_perf.cc
 *
 *
 *  Created by Daniel Tralamazza on 4/15/08.
 *  Copyright 2008. All rights reserved.
 *
 */

#include "test_perf.h"
#include <unistd.h>
#include <signal.h>
#include <sys/resource.h>


static pthread_mutex_t lockNotify;
static pthread_cond_t condNotify;
volatile static int countNotify;

static vector<pthread_mutex_t> LOCKS;
static vector<PerfThread> THREADS;
static int DELAY_IN;
static int DELAY_OUT;
static int MAX_DEPTH;
static int MAX_ITERATIONS;


__inline__ uint64_t rdtsc() {
	uint32_t lo, hi;
	__asm__ __volatile__ (
		"xorl %%eax,%%eax \n cpuid"
		::: "%rax", "%rbx", "%rcx", "%rdx");
	__asm__ __volatile__ ("rdtsc" : "=a" (lo), "=d" (hi));
	return (uint64_t)hi << 32 | lo;
}

static void _nnsleep(int sec, int nsec) {
	struct timespec ts;
	ts.tv_sec = sec;
	ts.tv_nsec = nsec;
	while (nanosleep(&ts, &ts) == -1) {}
}

#define CALIB_TIME	100000
static uint64_t TICKS = 0;
static void micro_sleep(int micro) {
	if (micro <= 0)
		return;
//	else if (micro < 1000) {
	uint64_t t = rdtsc() + (TICKS / (CALIB_TIME / micro));
	while (rdtsc() < t) {}
/*	} else {
		struct timespec ts;
		ts.tv_sec = 0;
		ts.tv_nsec = 1000 * micro;
		while (nanosleep(&ts, &ts) == -1) {}
	}*/
}

static void calibrate(int iter) {
	uint64_t dt;
	struct timespec ts;
	ts.tv_sec = 0;
	ts.tv_nsec = 1000 * CALIB_TIME;
	while (iter-- > 0) {
		dt = rdtsc();
		while (nanosleep(&ts, &ts) == -1) {}
		dt = rdtsc() - dt;
		TICKS = (TICKS * 0.05) + (dt * 0.95);
	}
}


#define N_FUNC 10

#define JUMP_CASE\
	switch (func_idx) {\
	case 0: f0(); break;\
	case 1: f1(); break;\
	case 2: f2(); break;\
	case 3: f3(); break;\
	case 4: f4(); break;\
	case 5: f5(); break;\
	case 6: f6(); break;\
	case 7: f7(); break;\
	case 8: f8(); break;\
	case 9: f9(); break;\
	}


#define JUMP_FUNC(i)\
	void PerfThread::f##i() {\
		pthread_testcancel();\
		int r = rand_r(&seed);\
		func_idx = r % N_FUNC;\
		if (depth == 1) {\
			pthread_mutex_lock(&LOCKS[lock_idx]);\
			++synch_count;\
			micro_sleep(DELAY_IN);\
			pthread_mutex_unlock(&LOCKS[lock_idx]);\
			pthread_testcancel();\
			r = rand_r(&seed);\
			lock_idx = r % LOCKS.size();\
			micro_sleep(DELAY_OUT);\
			return;\
		}\
		--depth;\
		JUMP_CASE\
	}


JUMP_FUNC(0)
JUMP_FUNC(1)
JUMP_FUNC(2)
JUMP_FUNC(3)
JUMP_FUNC(4)
JUMP_FUNC(5)
JUMP_FUNC(6)
JUMP_FUNC(7)
JUMP_FUNC(8)
JUMP_FUNC(9)


void* PerfThread::thr_run(void* arg) {
	PerfThread* t = (PerfThread*)arg;
//	pthread_setcanceltype(PTHREAD_CANCEL_ASYNCHRONOUS, 0);

	t->running = true;

	pthread_mutex_lock(&lockNotify);
	--countNotify;
	while (countNotify > 0)
		pthread_cond_wait(&condNotify, &lockNotify);
	pthread_mutex_unlock(&lockNotify);

	for (;;) {
		if (!t->running)
			break;
		t->depth = MAX_DEPTH;
		t->jump();
	}
	return NULL;
}

void PerfThread::jump() {
	JUMP_CASE
}


int main(int argc, char** argv) {
	if (argc < 7) {
		cout << "usage: " << argv[0] << " <threads> <locks> <delay_in> <delay_out> <stack depth> <duration in sec>" << endl;
		return 1;
	}

	int nthreads = atoi(argv[1]);
	int nlocks = atoi(argv[2]);
	DELAY_IN = atoi(argv[3]);
	DELAY_OUT = atoi(argv[4]);
	MAX_DEPTH = atoi(argv[5]);
	int duration = atoi(argv[6]);

	calibrate(1);

	countNotify = nthreads + 1;

	struct timeval tv;
	gettimeofday(&tv, NULL);
	srand(tv.tv_usec);
	rand();

	for (int i = 0; i < nthreads; ++i)
		THREADS.push_back(PerfThread(i, i % nlocks));

	LOCKS.resize(nlocks);
	for (int i = 0; i < nlocks; ++i)
		pthread_mutex_init(&LOCKS[i], 0);

	for (vector<PerfThread>::iterator it = THREADS.begin(); it != THREADS.end(); ++it) {
		if (!(*it).start()) {
			cerr << "could not create thread, sleeping 10ms" << endl;
			struct timespec ts;
			ts.tv_sec = 0;
			ts.tv_nsec = 10 * 1000 * 1000;
			while (nanosleep(&ts, &ts) == -1) {}
			if (!(*it).start()) {
				cerr << "error again! I give up" << endl;
				exit(1);
			}
		}
	}

	for (;;) {
		_nnsleep(0, 10 * 1000 * 1000);
		//sched_yield();
		pthread_mutex_lock(&lockNotify);
		if (countNotify == 1) {
			--countNotify;
			pthread_cond_broadcast(&condNotify);
			pthread_mutex_unlock(&lockNotify);
			break;
		}
		pthread_mutex_unlock(&lockNotify);
	}


	struct timeval tv_start;
	gettimeofday(&tv_start, NULL);


	_nnsleep(duration, 0);


	for (vector<PerfThread>::iterator it = THREADS.begin(); it != THREADS.end(); ++it)
		(*it).cancel();

	long long sync_count = 0;
	for (vector<PerfThread>::iterator it = THREADS.begin(); it != THREADS.end(); ++it)
		sync_count += (*it).wait();

	struct timeval tv_finish;
	gettimeofday(&tv_finish, NULL);
	double elapsed = tv_finish.tv_sec - tv_start.tv_sec + (tv_finish.tv_usec - tv_start.tv_usec) / 1.e6;
	cout.precision(5);

	struct rusage u;
	getrusage(RUSAGE_SELF, &u);

	cout << nthreads << "\t" << nlocks << "\t" << DELAY_IN << "\t" << DELAY_OUT << "\t" <<elapsed << '\t' << sync_count << '\t' << fixed << sync_count / elapsed << '\t' << u.ru_maxrss << '\t' << (double)u.ru_utime.tv_sec + (u.ru_utime.tv_usec / 1000000.0) << '\t' << (double)u.ru_stime.tv_sec + (u.ru_stime.tv_usec / 1000000.0) << '\t' << u.ru_nivcsw << '\t' << u.ru_nvcsw << endl;

	return 0;
}
