/*
 *  yieldcause.h
 *
 *
 *  Created by Daniel Tralamazza on 10/6/08.
 *  Copyright 2008. All rights reserved.
 *
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
	YieldCause(Position* p, Thread* t, Mutex *m) : pos(p), thread(t), mutex(m)  {}

	/* copy ctor */
	YieldCause(const YieldCause &yc) : pos(yc.pos), thread(yc.thread), mutex(yc.mutex) {}

	/* assign operator */
	YieldCause& operator = (const YieldCause &yc) {
		if (this != &yc) {
			pos = yc.pos;
			thread = yc.thread;
			mutex = yc.mutex;
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
