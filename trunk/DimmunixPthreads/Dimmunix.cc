/*
 * Dimmunix.cpp
 *
 *  Created on: Sep 17, 2009
 *      Author: horatiu
 */

#include "Dimmunix.h"

Dimmunix::Dimmunix() {
	initLock.lock();
	initialized = false;
	exiting = false;
	init();
	initLock.unlock();
}

Dimmunix::~Dimmunix() {
	exiting = true;
	pthread_rwlock_destroy(&hlock);
	pthread_key_delete(pkey);
}

void Dimmunix::init() {
	printf_nonblocking("init\n");
	real_pthread_mutex_lock = (int (*)(pthread_mutex_t*))dlsym(RTLD_NEXT, "pthread_mutex_lock");
	real_pthread_mutex_trylock = (int (*)(pthread_mutex_t*))dlsym(RTLD_NEXT, "pthread_mutex_trylock");
	real_pthread_mutex_unlock = (int (*)(pthread_mutex_t*))dlsym(RTLD_NEXT, "pthread_mutex_unlock");
	pthread_key_create(&pkey, NULL);
	pthread_rwlock_init(&hlock, NULL);
	initialized = true;
}



