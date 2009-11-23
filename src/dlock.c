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

#include "dlock.h"

/* These functions must be specific for each OS/libpthread */

#ifdef __FreeBSD__
/* thr_private.h contains pthread_mutex and pthread structs */
#include "thr_private.h"

dlock_mutex_t* dlock_mutex(pthread_mutex_t* pmtx) {
	return ((struct pthread_mutex*)*pmtx)->dlock_mtx;
}

dlock_thread_t* dlock_thread(pthread_t* pthr) {
	return ((struct pthread*)*pthr)->dlock_thr;
}

void dlock_set_thread(pthread_t* pthr, dlock_thread_t* thr) {
	((struct pthread*)*pthr)->dlock_thr = thr;
}
#endif


