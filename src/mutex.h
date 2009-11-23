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


#ifndef __DLOCK_MTX_H
#define __DLOCK_MTX_H

#include <pthread.h>
#include <stdint.h>
#include "position.h"
#include "util.h"
#include "thread.h"

namespace dlock {

class Thread;

class Mutex {
public:
	Mutex(pthread_mutex_t* _pmtx) : pos(0), owner(0), count(0), fAvoiding(true) {}

	void avoid_toggle(const bool v) { fAvoiding = v; }
	bool avoiding() const { return fAvoiding; }

	Position* pos;			/* acqPos[l] */
	Thread* owner;	/* current owner otherwise null */
	volatile int count;
private:
	/* copy ctor */
	Mutex(Mutex &m) {}
	/* assign operator */
	Mutex& operator = (Mutex &m) { return *this; }

	volatile bool fAvoiding;	/* flag */
};


}; // namespace dlock

typedef dlock::Mutex dlock_mutex_t;

#endif
