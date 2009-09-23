/*
 * Dimmunix.h
 *
 *  Created on: Sep 18, 2009
 *      Author: horatiu
 */

#ifndef DIMMUNIX_H_
#define DIMMUNIX_H_

#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <dlfcn.h>
#include <time.h>
#include <ext/hash_map>

#include "ulock.h"
#include "ThreadInfo.h"
#include <linux/unistd.h>
#include "dlock.h"
#include "hash.h"
#include "mutex.h"

using namespace std;
using namespace __gnu_cxx;
using namespace dlock;

#define NTHREADS 65536

extern int (*real_pthread_mutex_lock) (pthread_mutex_t *lock);
extern int (*real_pthread_mutex_trylock) (pthread_mutex_t *lock);
extern int (*real_pthread_mutex_unlock) (pthread_mutex_t *lock);
extern volatile bool exiting;
extern volatile bool avoidanceInitialized;
extern volatile bool detectionInitialized;

extern ULock globalLock;

class Dimmunix {
public:
	Dimmunix();
	virtual ~Dimmunix();

	volatile bool initialized;

	ThreadInfo* threadsInfo;//thread local storage
	volatile int nthreads;

	hash_map<pthread_mutex_t*, dlock_mutex_t*, dlock::hash<pthread_mutex_t*> > locksMap;

	void init();
};

extern Dimmunix dimmunix;

#endif /* DIMMUNIX_H_ */
