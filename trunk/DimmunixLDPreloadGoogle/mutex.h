/*
 *  mutex.h
 *
 *
 *  Created by Daniel Tralamazza on 4/12/08.
 *  Copyright 2008. All rights reserved.
 *
 */

#ifndef __DLOCK_MTX_H
#define __DLOCK_MTX_H

#include <pthread.h>
#include <stdint.h>
#include "position.h"
#include "util.h"


namespace dlock {


class Mutex {
public:
	Mutex(pthread_mutex_t* _pmtx) : pos(0), fPmtx(_pmtx), fAvoiding(true), fContentions(0) {}

	void avoid_toggle(const bool v) { fAvoiding = v; }
	bool avoid_enabled() const { return fAvoiding; }
	void inc_contentions() { ATOMIC_INC(fContentions); }

	Position* pos;			/* acqPos[l] */
private:
	/* copy ctor */
	Mutex(Mutex &m) {}
	/* assign operator */
	Mutex& operator = (Mutex &m) { return *this; }

	pthread_mutex_t* fPmtx;		/* original mutex */
	volatile bool fAvoiding;	/* flag */
	uint32_t fContentions;	/* number of contentions */
};


}; // namespace dlock

typedef dlock::Mutex dlock_mutex_t;

#endif
