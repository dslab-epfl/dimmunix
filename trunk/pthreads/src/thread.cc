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

#include "thread.h"
#include "dlock.h"


namespace dlock {

Thread::Thread() : zombie(false), bypass_avoidance(false) {
	pthread_cond_init(&fYieldCond, 0);
	pthread_mutex_init(&fYieldMtx, 0);
	/* disable avoidance for the yield lock */
	dlock_mutex_disable_avoidance(&fYieldMtx);
#ifdef __FreeBSD__
	pthread_cleanup_push(Thread::dlock_thread_cleanup, this);
#endif
}

Thread::~Thread() {
#ifdef __FreeBSD__
	pthread_cleanup_pop(1);
#endif
	pthread_mutex_destroy(&fYieldMtx);
	pthread_cond_destroy(&fYieldCond);
}

/* thread waits (blocks) if it has something in its yield_cause */
void Thread::yield_wait() {
	real_pthread_mutex_lock(&fYieldMtx);				/* native_lock(yieldLock[t]) */
	while (!yield_cause.empty() && !bypass_avoidance)	/* if yieldCause[t] != null */
		pthread_cond_wait(&fYieldCond, &fYieldMtx);	/* yieldLock[t].wait */
	real_pthread_mutex_unlock(&fYieldMtx);			/* native_unlock(yieldLock[t]) */
}

/* if yield_cause is not empty wakes up this thread */
void Thread::yield_notify() {
	if (!yield_cause.empty()) {
//		real_pthread_mutex_lock(&fYieldMtx);		/* native_lock(yieldLock[t']) */
		yield_cause.clear();			/* yieldCause[t'] = null */
		pthread_cond_signal(&fYieldCond);	/* yieldLock[t'].notify */
		DLOCK_DEBUGF("%p notifies\n", pthread_self());
//		real_pthread_mutex_unlock(&fYieldMtx);	/* native_unlock(yieldLock[t']) */
	}
}

void Thread::acquire(Mutex* m) {
	foreach(vector<Mutex*>::iterator, l, locksHeld)
		innerLocks[*l].push_back(m);

	locksHeld.push_back(m);
}

void Thread::release(Mutex* m, unsigned maxFPs) {
	foreach(vector<Mutex*>::iterator, l, locksHeld) {
		if (*l == m) {
			locksHeld.erase(l);
			break;
		}
	}

	//check if it was FP/TP
	vector<Mutex*>& locks = innerLocks[m];
	vector<TemplateInstance>& v_inst = instances[m];

	foreach(vector<TemplateInstance>::iterator, inst, v_inst) {
		if (!inst->tmpl->enabled)
			continue;
		vector<Mutex*>& locks_other = inst->peerLocks;
		Mutex* m_other = inst->tupples[0].mutex;
		if (lockInversion(m, locks, m_other, locks_other)) {
			inst->tmpl->nTPs++;
		}
		else {
			inst->tmpl->nFPs++;
			if (inst->tmpl->nFPs >= maxFPs)
				inst->tmpl->enabled = false;
		}
	}

	locks.clear();
	v_inst.clear();
}

bool Thread::lockInversion(Mutex* m1, vector<Mutex*>& v1, Mutex* m2, vector<Mutex*>& v2) {
	bool inv1 = false;
	bool inv2 = false;

	for (unsigned i = 0; i < v1.size(); i++) {
		if (v1[i] == m2) {
			inv1 = true;
			break;
		}
	}

	if (!inv1)
		return false;

	for (unsigned i = 0; i < v2.size(); i++) {
		if (v2[i] == m1) {
			inv2 = true;
			break;
		}
	}

	return inv2;
}


}; // namespace dlock

