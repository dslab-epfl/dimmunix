#include <algorithm>
#include <fstream>
#include <sstream>

#include <unistd.h>

#include "avoidance.h"
#include "util.h"
#include "dlock.h"
#include <dlfcn.h>


/* MAIN GLOBAL VARIABLE */
dlock::Avoidance gAvoid;

/* API functions in dlock.h */

dlock::Thread* dlock_thread_create() {
	return gAvoid.alloc_thread();
}

void dlock_acquire(dlock::Thread* thr, dlock::Mutex* mtx, int non_blocking) {
	if (thr->in_dimmunix)
		return;
	thr->in_dimmunix = true;
	gAvoid.acquire(thr, mtx, non_blocking);
	thr->in_dimmunix = false;
}

void dlock_acquired(dlock::Thread* thr, dlock::Mutex* mtx) {
	if (thr->in_dimmunix)
		return;
	thr->in_dimmunix = true;
	gAvoid.acquired(thr, mtx);
	thr->in_dimmunix = false;
}

void dlock_release(dlock::Thread* thr, dlock::Mutex* mtx) {
	if (thr->in_dimmunix)
		return;
	thr->in_dimmunix = true;
	gAvoid.release(thr, mtx);
	thr->in_dimmunix = false;
}

void dlock_cancel(dlock::Thread* thr, dlock::Mutex* mtx) {
	if (thr->in_dimmunix)
		return;
	thr->in_dimmunix = true;
	gAvoid.cancel(thr, mtx);
	thr->in_dimmunix = false;
}

namespace dlock {

#define ALLOWED_CAPACITY 64
#define YIELDERS_CAPACITY 64
#define CACHE_POSITION_CAPACITY 50000
// this skips the pthread functions
#ifndef STACK_OVERHEAD
#define STACK_OVERHEAD 2
#endif

Avoidance::Avoidance() : enable_dynamic_analysis(false),
	default_stack_depth(DIMMU_MAX_STACK_DEPTH),
	default_match_depth(DIMMU_MAX_STACK_DEPTH),
	enabled(false),
	disable_template_instance(false),
	yield_count(0) {

	pthread_mutex_init(&avoidLock, 0);
	dlock_mutex_disable_avoidance(&avoidLock);

//	position_cache.resize(CACHE_POSITION_CAPACITY);
//	allowed.resize(ALLOWED_CAPACITY);
//	yielders.resize(YIELDERS_CAPACITY);

	if (getenv("DIMMU_AVOIDANCE_DISABLE") != NULL) {
		disable_template_instance = true;
		DLOCK_DEBUGF("disabling avoidance\n");
	}
	char* tmp;
	if ((tmp = getenv("DIMMU_STACK_DEPTH")) != NULL) {
		default_stack_depth = atoi(tmp);
		DLOCK_DEBUGF("stack depth set to %d\n", default_stack_depth);
	}
	if ((tmp = getenv("DIMMU_MATCH_DEPTH")) != NULL) {
		default_match_depth = atoi(tmp);
		DLOCK_DEBUGF("match depth set to %d\n", default_match_depth);
	} else
		default_match_depth = default_stack_depth;
	if ((tmp=getenv("ENABLE_DYNAMIC_ANALYSIS")) != NULL) {
		enable_dynamic_analysis = (bool)atoi(tmp);
	}


	/* load our own template history */
	TemplateFactory::load_templates(history, HISTORY_FNAME);

	/* build the template position map */
	foreach(vector<Template>::iterator, it, history) {
		foreach(vector<Position>::iterator, it_p, (*it).positions) {
			(*it_p).rehash(default_match_depth); /* recompute hashcode */
			history_positions[(*it_p).hashcode].push_back(&*it);
		}
	}

	/* only after all init we can enable */
	enabled = true;

	//for microbench
	curSig = 0;
	tlastInstUsec = currentTimeUsec();
	if ((tmp = getenv("DELTA_INST_USEC")) != NULL)
		deltaInstUsec = atoi(tmp);
	else
		deltaInstUsec = 0;
	if ((tmp = getenv("MAX_FP_SIG")) != NULL)
		maxFPsPerSig = atoi(tmp);
	else
		maxFPsPerSig = 0;

	monitor.history_av = &history;
}

Avoidance::~Avoidance() {
	enabled = false;

	char* tmp = 0;
	if ((tmp = getenv("DIMMU_GEN_RAND_TEMPLATES")) != NULL)
		generate_templates(atoi(tmp));

	foreach(PositionCacheMap::iterator, it, position_cache)
		delete (*it).second; /* delete positions */

	pthread_mutex_destroy(&avoidLock);

//	foreach(vector<Template>::iterator, tmpl, history) {
//		printf("sigId = %d, enabled = %d, #FPs = %d, #TPs = %d\n", tmpl->id, tmpl->enabled, tmpl->nFPs, tmpl->nTPs);
//	}
}

Position* Avoidance::getPos(int id) {
	char s[10];
	sprintf(s, "f%dii", id);

	foreach(PositionCacheMap::iterator, it, position_cache) {
		Position* p = it->second;
		void* addr = p->trace.stack[1];
		Dl_info info;
		dladdr(addr, &info);
		const char* sname = info.dli_sname;
		if (strstr(sname, s) != NULL)
			return p;
	}
	return NULL;
}

void Avoidance::generate_templates(int n) {
	/* first get X number of 'real' positions from the benchmark
	 * sample the positions 2* n times to form the templates
	 */
	int ptotal = n * (n - 1); // number of different positions
	vector<Position*> vpos;
	PositionCacheMap::iterator it = position_cache.begin();
	for (int i = 0; i < ptotal; ++i) {
		if (it == position_cache.end())
			break;
		vpos.push_back((*it++).second);
	}
	if (vpos.size() < 2) {
		DLOCK_DEBUGF("not enough different positions %d\n", vpos.size());
	} else {
		srand(vpos.size());
		rand();
		Template t;
		history.clear();
		Position *p1, *p2;
		int sigid = 0;
		for (int i = 1; i <= 20; i++)
			for (int j = i; j <= 20; j++) {
				if (sigid == n)
					goto end_loop;
				t.clear();
				//			p1 = vpos[rand() % vpos.size()];
				//			while ((p2 = vpos[rand() % vpos.size()]) == p1) {}
				p1 = getPos(i);
				p2 = getPos(j);
				t.positions.push_back( *p1 );
				t.positions.push_back( *p2 );
				history.push_back(t);
				sigid++;
			}

		end_loop:
		ostringstream os;
		os << "dlock.history";
		DLOCK_DEBUGF("%d fake templates generated\n", n);
		TemplateFactory::save_templates(history, os.str().c_str());
	}
}

void Avoidance::acquire(Thread* thr, Mutex* mtx, int non_blocking) {
	if (!mtx->avoiding() || !enabled)
		return;

	StackTrace st;
	st.match_depth = default_match_depth;
	st.capture(default_stack_depth); /* capture stack trace */

	plock.lock();
	Position* &p = position_cache[hash_value(st)];
	plock.unlock();

	if (p == NULL) /* new position */
		p = new Position(st);

	thr->lockPos = p; /* small hack (#1) to pass the lock position */

	/* trylock/timedlock should not add edges because they cannot deadlock */
	if (non_blocking)
		return;

	thr->enqueue_event(p, mtx, Event::REQUEST);

	while (yield_request(thr, mtx, p)) {
//		cout << "yielding" << endl;
		thr->yield_wait();
		if (enable_dynamic_analysis) {
			if (thr->currentInstance.tmpl->enabled)
				thr->instances[mtx].push_back(thr->currentInstance);
		}
	}
}

void Avoidance::acquired(Thread* thr, Mutex* mtx) {
	if (!mtx->avoiding() || !enabled)
		return;

	mtx->pos = thr->lockPos; /* (#1) acqPos[l] = p */
	mtx->owner = thr;
	thr->enqueue_event(thr->lockPos, mtx, Event::ACQUIRED);
	if (enable_dynamic_analysis) {
		thr->acquire(mtx);
	}
}

void Avoidance::release(Thread* thr, Mutex* mtx) {
	if (!mtx->avoiding() || !enabled)
		return;

	if (mtx->pos == NULL) {
		DLOCK_DEBUGF("%p mutex position NULL\n", pthread_self());
		return; /* this is bad and should not happen */
	}

	Position *p = mtx->pos;
	mtx->pos = NULL; /* acqPos[l] = null */
	mtx->owner = NULL;
	thr->enqueue_event(p, mtx, Event::RELEASE);

	pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */
	remove_allowed(thr, mtx, p);
	YieldCause yc(p, thr, mtx, 0);
	if (yielders.find(yc) != yielders.end()) {
		vector<Thread*> &vthr =  yielders[yc];
		foreach(vector<Thread*>::iterator, it, vthr) {
			if (enable_dynamic_analysis && (*it)->currentInstance.tmpl->enabled) {
				vector<Mutex*>& v_inner = thr->innerLocks[mtx];
				(*it)->currentInstance.peerLocks.assign(v_inner.begin(), v_inner.end());
			}
			(*it)->yield_notify(); /* notify */
		}
		yielders.erase(yc);
	}
	if (enable_dynamic_analysis) {
		thr->release(mtx, maxFPsPerSig);
	}
	pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */

	thr->lockPos = NULL; /* (#1) reset thr lock position */

}

void Avoidance::cancel(Thread* thr, Mutex* mtx) {
	if (!mtx->avoiding() || !enabled)
		return;

	thr->enqueue_event(thr->lockPos, mtx, Event::CANCEL);

	pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */
	remove_allowed(thr, mtx, thr->lockPos);
	pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */

	thr->lockPos = NULL; /* (#1) reset thr lock position */
}

bool Avoidance::yield_request(Thread* thr, Mutex* mtx, Position* pos) {
	pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */

	template_instance(thr, mtx, pos); /* try to instance a template */

	if (thr->yield_cause.empty() || thr->bypass_avoidance) { /* if yieldCause[t] == null */
		if (thr->bypass_avoidance)
			thr->bypass_avoidance = false; /* we bypass only once */

		insert_allowed(thr, mtx, pos); /* lockGrantees[p] += t */
		pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */
		thr->enqueue_event(pos, mtx, Event::GRANT);
		return false; /* return OK */
	} else {
		/* foreach (t',p') in yieldCause[t] */
		foreach(vector<YieldCause>::iterator, yc_it, thr->yield_cause)
			yielders[*yc_it].push_back(thr); /* yielders[t',p'] += t */
		yield_count++;
		thr->currentInstance.tmpl->nInst++;
		pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */
		//for microbench
//		if (deltaInstUsec > 0) {
//			check:
//			while (currentTimeUsec()- tlastInstUsec < deltaInstUsec)
//				pthread_yield();
//			pthread_mutex_lock(&avoidLock); /* native_lock(avoidanceLock) */
//			if (currentTimeUsec()- tlastInstUsec < deltaInstUsec) {
//				pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */
//				goto check;
//			}
//			else {
//				tlastInstUsec = currentTimeUsec();
//				pthread_mutex_unlock(&avoidLock); /* native_unlock(avoidanceLock) */
//			}
//		}
		thr->enqueue_yield_event(pos, mtx, thr->yield_cause);
//		if (enable_dynamic_analysis)
//			monitor.addInstance(thr->currentInstance);
		return true; /* return YIELD */
	}
}

void Avoidance::remove_allowed(Thread* t, Mutex* m, Position* p) {
	vector<LockGrant> &vtmp = allowed[p->hashcode];
	foreach(vector<LockGrant>::iterator, it, vtmp)
		if ( ((*it).thread == t) && (*it).mutex == m ) {
			vtmp.erase(it);
			break;
		}
}

void Avoidance::insert_allowed(Thread* t, Mutex* m, Position* p) {
	vector<LockGrant> &vtmp = allowed[p->hashcode];
	vtmp.push_back(LockGrant(t, m, 0)); /* lockGrantees[p] += t,m */
}

bool Avoidance::match_template(TemplateInstance &ti) {
	foreach(vector<YieldCause>::iterator, it_yc, ti.tupples) {
		vector<LockGrant> &vtmp = allowed[(*it_yc).pos->hashcode]; /* thr,mtx at it_yc.pos */
		if (vtmp.size() == 0)
			return false; /* no thread, mutex pairs, return */
		/* TODO: ugly, please refactor this */
		foreach(vector<LockGrant>::iterator, it_tm, vtmp) {
			if ( ((*it_yc).thread == NULL) && ((*it_yc).mutex != (*it_tm).mutex) ) {
				(*it_yc).thread = (*it_tm).thread;
				(*it_yc).mutex = (*it_tm).mutex;
				(*it_yc).time = (*it_tm).time;
				ti.match();
				if (ti.all_match())
					return true;
			}
		}
	}
	return false;
}

void Avoidance::template_instance(Thread* thr, Mutex* mtx, Position* pos) {
	vector<Template*> &vtmpl = history_positions[pos->hashcode]; /* templates that contain pos->hashcode */
	foreach(vector<Template*>::iterator, it_T, vtmpl) {
		if (!(*it_T)->enabled) {
			continue;
		}
		TemplateInstance ti(*it_T, thr, mtx, pos, 0);
		if (match_template(ti)) {
			if (!disable_template_instance) {
				thr->yield_cause.assign(ti.tupples.begin(), ti.tupples.end()); /* yield_cause fill */
				thr->currentInstance = ti;
			}
			DLOCK_DEBUGF("match found, t.size=%d\n", ti.tupples.size());
			break; /* match found */
		}
	}
}


}; // namespace

