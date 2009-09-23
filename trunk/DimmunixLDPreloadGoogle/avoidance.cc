#include <algorithm>
#include <fstream>

#include <unistd.h>

#include "avoidance.h"
#include "util.h"
#include "dlock.h"
#include "Dimmunix.h"

/* MAIN GLOBAL VARIABLE */
static dlock::Avoidance gAvoid;

/* API functions in dlock.h */

dlock::Thread* dlock_thread_create() {
	return gAvoid.create_thread();
}

void dlock_acquire(dlock::Thread* thr, dlock::Mutex* mtx, int non_blocking) {
	if ((thr != 0) && (mtx != 0))
		gAvoid.acquire(thr, mtx, non_blocking);
}

void dlock_acquired(dlock::Thread* thr, dlock::Mutex* mtx) {
	if ((thr != 0) && (mtx != 0))
		gAvoid.acquired(thr, mtx);
}

void dlock_release(dlock::Thread* thr, dlock::Mutex* mtx) {
	if ((thr != 0) && (mtx != 0))
		gAvoid.release(thr, mtx);
}

void dlock_cancel(dlock::Thread* thr, dlock::Mutex* mtx) {
	if ((thr != 0) && (mtx != 0))
		gAvoid.cancel(thr, mtx);
}

namespace dlock {

#define THREADS_INITIAL_CAPACITY 5000
#define CACHE_POSITION_CAPACITY 10000
// this skips the pthread functions
#ifndef STACK_OVERHEAD
#define STACK_OVERHEAD 2
#endif
#define DIFFERENT_POSITIONS_TO_SAVE 10
#define DIMMU_COUNTERS_FNAME "dlock.counters"
#define DIMMU_YIELDS_FNAME "dlock.yields"

Avoidance::Avoidance() :
	enabled(true), disable_template_instance(false), default_stack_depth(
			DIMMU_MAX_STACK_DEPTH), default_match_depth(DIMMU_MAX_STACK_DEPTH),
			yield_count(0) {

	pthread_mutex_init(&avoidLock, 0);
	dlock_mutex_disable_avoidance(&avoidLock);

	pthread_mutex_init(&threadsLock, 0);
	dlock_mutex_disable_avoidance(&threadsLock);

	pthread_mutex_init(&positionLock, 0);
	dlock_mutex_disable_avoidance(&positionLock);

	threads.reserve(THREADS_INITIAL_CAPACITY);
	position_cache.resize(CACHE_POSITION_CAPACITY);

	monitor = new Detection();

	if (getenv("DIMMU_AVOIDANCE_DISABLE") != NULL) {
		disable_template_instance = true;
		DLOCK_DEBUGF("disabling avoidance\n");
	}
	char* tmp;
	if ((tmp = getenv("DIMMU_STACK_DEPTH")) != NULL) {
		default_stack_depth = atoi(tmp) + STACK_OVERHEAD;
		DLOCK_DEBUGF("stack depth set to %d\n", default_stack_depth);
	}
	if ((tmp = getenv("DIMMU_MATCH_DEPTH")) != NULL) {
		default_match_depth = atoi(tmp) + STACK_OVERHEAD;
		DLOCK_DEBUGF("match depth set to %d\n", default_match_depth);
	} else
		default_match_depth = default_stack_depth;

	/* load our own template history */
	TemplateFactory::load_templates(history, HISTORY_FNAME);

	/* build the template position map */
	foreach(vector<Template>::iterator, it, history)
		foreach(vector<Position>::iterator, it_p, (*it).positions) {
			(*it_p).rehash(default_match_depth); /* recompute hashcode */
			history_positions[(*it_p).hashcode].push_back(&*it);
		}

	avoidanceInitialized = true;
}

Avoidance::~Avoidance() {
	enabled = false;

/*	char* tmp = 0;
	if ((tmp = getenv("DIMMU_SAVE_COUNTERS")) != NULL) {
		ofstream ofile;
		ofile.open(DIMMU_COUNTERS_FNAME);
		ofile << yield_count << endl;
		ofile.close();
	}
#ifdef SAVE_YIELD_POS
#if 0
	ofstream ofile;
	ofile.open(DIMMU_YIELDS_FNAME);
	typedef map<Position::hash_t, int> PositionCountMap;
	PositionCountMap distinct_yield;
	foreach(vector<Template>::iterator, it_t, history) {
		distinct_yield.clear(); // count distinct yield positions
		foreach(vector<Position*>::iterator, it_p, (*it_t).yield_positions)
		distinct_yield[(*it_p)->hashcode] += 1;

		int largest = 0; // get the largest value
		foreach(PositionCountMap::iterator, it_p_c, distinct_yield)
		if (largest < (*it_p_c).second)
		largest = (*it_p_c).second;

		// "total number of yields" and "largest number of yields on a position"
		ofile << (*it_t).yield_positions.size() << " " << largest << endl;
	}
	ofile.close();
#endif
	ofstream ofile;
	ofile.open(DIMMU_YIELDS_FNAME);
	unsigned int true_p = 0, false_p = 0;
	foreach(vector<Template>::iterator, it_t, history) {
		true_p += (*it_t).instantiations;
		false_p += (*it_t).false_instantiations;
	}
	ofile << true_p << " " << false_p << endl;
	ofile.close();
#endif
	if ((tmp = getenv("DIMMU_GEN_RAND_TEMPLATES")) != NULL) {
		// first get X number of 'real' positions from the benchmark
		// sample the positions 2* n times to form the templates
		// This doesn't guarantee real positions
		int n = atoi(tmp); // number of templates
		int ptotal = n * (n - 1); // number of different positions
		vector<Position*> vpos;
		PositionCacheMap::iterator it = position_cache.begin();
		for (int i = 0; i < ptotal; ++i) {
			if (it == position_cache.end())
				break;
			vpos.push_back((*it++).second);
		}
		if (vpos.size() > 1) {
			if (vpos.size() < DIFFERENT_POSITIONS_TO_SAVE)
				DLOCK_DEBUGF("not enough different positions %d\n", vpos.size());
			srand(vpos.size());
			rand();
			Template t;
			history.clear();
			Position *p1, *p2;
			for (int i = 0; i < n; ++i) {
				t.clear();
				//				t.positions.push_back( *vpos[rand() % vpos.size()] );
				//				t.positions.push_back( *vpos[rand() % vpos.size()] );
				p1 = vpos[rand() % vpos.size()];
				while ((p2 = vpos[rand() % vpos.size()]) == p1) {
				}
				t.positions.push_back(*p1);
				t.positions.push_back(*p2);
				history.push_back(t);
			}
			DLOCK_DEBUGF("%d fake templates generated\n", n);
			TemplateFactory::save_templates(history, HISTORY_FNAME);
		}
	}
	*/

	foreach(PositionCacheMap::iterator, it, position_cache)
		delete (*it).second; /* delete positions */
	pthread_mutex_destroy(&positionLock);

	delete monitor;

	pthread_mutex_destroy(&avoidLock);
	pthread_mutex_destroy(&threadsLock);
}

void Avoidance::acquire(Thread* thr, Mutex* mtx, int non_blocking) {
	if (!mtx->avoid_enabled() || !enabled)
		return;

	StackTrace st;
	st.match_depth = default_match_depth;
	st.capture(default_stack_depth); /* capture stack trace */

	real_pthread_mutex_lock(&positionLock);
	//Position* &p = position_cache[hash_value(st)];
	Position* &p = position_cache[hash_value_full(st)];
	real_pthread_mutex_unlock(&positionLock);

	if (p == NULL) { /* new position */
		p = new Position(st);
		//p->print_unlocked();
	}

	thr->lockPos = p; /* small hack (#1) to pass the lock position */

	/* trylock/timedlock should not add edges because they cannot deadlock */
	if (non_blocking)
		return;

	monitor->push_event(p, thr, mtx, Event::REQUEST); /* events += [request(t,l,p)] */

	while (yield_request(thr, mtx, p)) {
		DLOCK_DEBUGF("%p yielding at %u\n", pthread_self(), p->hashcode);
		thr->yield_wait();
	}
}

void Avoidance::acquired(Thread* thr, Mutex* mtx) {
	if (!mtx->avoid_enabled() || !enabled)
		return;

	mtx->pos = thr->lockPos; /* (#1) acqPos[l] = p */
	monitor->push_event(thr->lockPos, thr, mtx, Event::ACQUIRED); /* events += [acquired(t,l,p)] */
}

void Avoidance::release(Thread* thr, Mutex* mtx) {
	if (!mtx->avoid_enabled() || !enabled)
		return;

	if (mtx->pos == NULL) {
		DLOCK_DEBUGF("%p mutex position NULL\n", pthread_self());
		return; /* this is bad and should not happen */
	}

	Position *p = mtx->pos;
	mtx->pos = NULL; /* acqPos[l] = null */

	monitor->push_event(p, thr, mtx, Event::RELEASE); /* events += [release(t,l,p)] */

	real_pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */

	thr->remove_granted(p, mtx);

	if (!thr->contains_granted(p, mtx)) {
		YieldCause yc(p, thr, mtx);
		if (yielders[yc].size() > 0) {
			foreach(vector<Thread*>::iterator, it, yielders[yc])
				(*it)->yield_notify();
			yielders[yc].clear();
		}
	}
	real_pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */

	thr->lockPos = NULL; /* (#1) reset thr lock position */
}

void Avoidance::cancel(Thread* thr, Mutex* mtx) {
	if (!mtx->avoid_enabled() || !enabled)
		return;

	monitor->push_event(thr->lockPos, thr, mtx, Event::CANCEL);

	real_pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */
	thr->remove_granted(thr->lockPos, mtx);
	real_pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */

	thr->lockPos = NULL; /* (#1) reset thr lock position */
}

bool Avoidance::yield_request(Thread* thr, Mutex* mtx, Position* pos) {
	real_pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */

	template_instance(thr, mtx, pos); /* try to instance a template */

	if (thr->yield_cause.empty() || thr->bypass_avoidance) { /* if yieldCause[t] == null */
		if (thr->bypass_avoidance)
			thr->bypass_avoidance = false; /* we bypass only once */
		thr->add_granted(pos, mtx);
		real_pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */
		monitor->push_event(pos, thr, mtx, Event::GRANT); /* events += [grant(t,l,p)] */
		return false; /* return OK */
	} else {
		/* foreach (t',p') in yieldCause[t] */
		foreach(vector<YieldCause>::iterator, yc_it, thr->yield_cause)
			yielders[*yc_it].push_back(thr); /* yielders[t',p'] += t */
		++yield_count;
		real_pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */
		monitor->push_event(pos, thr, mtx, Event::YIELD); /* events += [yield(t,yieldCause[t])] */
		return true; /* return YIELD */
	}
}

bool Avoidance::match_template(TemplateInstance &ti) {
	foreach(vector<Thread*>::iterator, it_t, threads) {
		foreach(vector<Thread::PositionGrant>::iterator, it_p, (*it_t)->positions_granted)
			if (ti.match((*it_p).first, *it_t, (*it_p).second))
				break;
		if (ti.all_match())
			return true;
	}
	return ti.all_match();
}

void Avoidance::template_instance(Thread* thr, Mutex* mtx, Position* pos) {
	vector<Template*> &vtmpl = history_positions[pos->hashcode];
	foreach(vector<Template*>::iterator, it_T, vtmpl) {
		TemplateInstance ti(*it_T, thr, mtx, pos);
		if (match_template(ti)) {
			(*it_T)->instantiated(ti.false_positive());
			if (!disable_template_instance)
				thr->yield_cause.assign(ti.tupples().begin(),
						ti.tupples().end());
			DLOCK_DEBUGF("match found, t.size=%d\n", ti.tupples().size());
			break; /* match found */
		}
	}
}

void Avoidance::remove_thread(Thread* t) {
	/*	pthread_mutex_lock(&threadsLock);
	 pthread_mutex_lock(&avoidLock);
	 //vector_remove(threads, t);
	 // TODO search in yielders
	 pthread_mutex_unlock(&avoidLock);
	 pthread_mutex_unlock(&threadsLock);*/
}

/* this is necessary to cleanup thread references */
void Avoidance::thread_cleanup(void* arg) {
	if (arg)
		gAvoid.remove_thread((Thread*) arg);
}

Thread* Avoidance::create_thread() {
	Thread* t = new Thread();
//	pthread_cleanup_push(Avoidance::thread_cleanup, t);
//	pthread_cleanup_pop(Avoidance::thread_cleanup);
	real_pthread_mutex_lock(&threadsLock);
	threads.push_back(t);
	//	monitor.register_event_queue(t, );
	real_pthread_mutex_unlock(&threadsLock);
	return t;
}

};
// namespace

