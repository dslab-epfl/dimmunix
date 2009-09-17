/*
 * Dimmunix.h
 *
 *  Created on: Sep 17, 2009
 *      Author: horatiu
 */

#ifndef DIMMUNIX_H_
#define DIMMUNIX_H_

#include <dlfcn.h>
#include <pthread.h>
#include "dlock.h"
#include "uthash.h"
#include "util.h"
#include <linux/unistd.h>
#include "ulock.h"

extern volatile bool exiting;

extern pthread_key_t pkey;
extern pthread_rwlock_t hlock;

extern int (*real_pthread_mutex_lock) (pthread_mutex_t *__mutex);
extern int (*real_pthread_mutex_trylock) (pthread_mutex_t *__mutex);
extern int (*real_pthread_mutex_unlock) (pthread_mutex_t *__mutex);
extern ULock initLock;

class Dimmunix {
public:
	Dimmunix();
	virtual ~Dimmunix();

	volatile bool initialized;

	void init();
};

#endif /* DIMMUNIX_H_ */
