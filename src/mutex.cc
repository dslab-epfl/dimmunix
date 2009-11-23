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

#include <pthread.h>
#include "mutex.h"
#include "dlock.h"


using namespace dlock;


Mutex* dlock_mutex_create(pthread_mutex_t* pmtx) {
	return new Mutex(pmtx);
}

void dlock_mutex_destroy(Mutex* mtx) {
	delete mtx;
}

void dlock_mutex_contention(Mutex* mtx) {
//	if (mtx)
//		mtx->inc_contentions();
}

void dlock_mutex_enable_avoidance(pthread_mutex_t* pmtx) {
	if (Mutex* m = dlock_mutex(pmtx))
		m->avoid_toggle(true);
}

void dlock_mutex_disable_avoidance(pthread_mutex_t* pmtx) {
	if (Mutex* m = dlock_mutex(pmtx))
		m->avoid_toggle(false);
}

