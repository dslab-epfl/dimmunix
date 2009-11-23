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


#ifndef __DLOCK_YIELDCAUSE_H
#define __DLOCK_YIELDCAUSE_H

#include <stdint.h>
#include "position.h"
#include "util.h"
#include "hash.h"
#include "thread.h"
#include "mutex.h"


namespace dlock {

class Thread;
class Mutex;

class YieldCause {
public:
	YieldCause(Position* p, Thread* t, Mutex *m, long _time) : pos(p), thread(t), mutex(m), time(_time)  {}

	/* copy ctor */
	YieldCause(const YieldCause &yc) : pos(yc.pos), thread(yc.thread), mutex(yc.mutex), time(yc.time) {}

	/* assign operator */
	YieldCause& operator = (const YieldCause &yc) {
		if (this != &yc) {
			pos = yc.pos;
			thread = yc.thread;
			mutex = yc.mutex;
			time = yc.time;
		}
		return *this;
	}

	bool operator == (const YieldCause &yc) const {
		return (pos->hashcode == yc.pos->hashcode) && (thread == yc.thread) && (mutex == yc.mutex);
	}

	bool operator < (const YieldCause &yc) const {
		if (pos->hashcode < yc.pos->hashcode)
			return true;
		else if (pos->hashcode > yc.pos->hashcode)
			return false;
		else {
			if (mutex < yc.mutex)
				return true;
			else if (mutex > yc.mutex)
				return false;
			else
				return thread < yc.thread;
		}
	}

	Position* pos;
	Thread* thread;
	Mutex* mutex;
	long time;
};

inline size_t hash_value(YieldCause const &yc) {
	size_t seed = 0;
	hash_combine(seed, yc.pos->hashcode);
	hash_combine(seed, yc.thread);
	hash_combine(seed, yc.mutex);
	return seed;
}

}; // namespace dlock

#endif
