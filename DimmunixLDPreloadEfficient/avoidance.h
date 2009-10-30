#ifndef __DLOCK_AVOIDANCE_H
#define __DLOCK_AVOIDANCE_H

#ifdef GOOGLE_HASH
#include <google/dense_hash_map>
#else
#include <tr1/unordered_map>
#endif

#include <map>
#include <set>
#include <vector>
#include <time.h>
#include <pthread.h>

#include "detection.h"
#include "thread.h"
#include "mutex.h"
#include "position.h"
#include "template.h"
#include "template_instance.h"
#include "hash.h"
#include "ulock.h"
#include "util.h"


#ifdef GOOGLE_HASH
using namespace google::dense_hash_map;
#define unordered_map dense_hash_map
#else
using namespace std::tr1;
#endif

namespace dlock {

class LockGrant {
public:
	Thread* thread;
	Mutex* mutex;
	long time;

	LockGrant(Thread* thread, Mutex* mutex, long time) {
		this->thread = thread;
		this->mutex = mutex;
		this->time = time;
	}
};

class Avoidance : public DetectionObserver {
	typedef unordered_map<Position::hash_t, Position*, hash<Position::hash_t> > PositionCacheMap;
	typedef unordered_map<Position::hash_t, vector<Template*>, hash<Position::hash_t> > PositionTemplatesMap;
	typedef unordered_map<YieldCause, vector<Thread*>, hash<YieldCause> > Yielders;
	typedef pair<Thread*, Mutex*> ThreadMutexPair;
	typedef unordered_map<Position::hash_t, vector<LockGrant>, hash<Position::hash_t> > AllowedMap;
public:
	Avoidance();
	~Avoidance();

	Thread* alloc_thread() {
		Thread* t = new Thread();
		monitor.register_thread(t);
		return t;
	}

	void acquire(Thread* thr, Mutex* mtx, int non_blocking);
	void acquired(Thread* thr, Mutex* mtx);
	void release(Thread* thr, Mutex* mtx);
	void cancel(Thread* thr, Mutex* mtx);

	/* notification from DetectionObserver */
	void on_deadlock() { enabled = false; }
	void on_livelock(Thread* t) {}

	void generate_templates(int n);

	//for microbench
	volatile int curSig;
	volatile long tlastInstUsec;
	volatile int deltaInstUsec;
	volatile unsigned maxFPsPerSig;
	bool enable_dynamic_analysis;

	/* template history */
	vector<Template> history;

	/* detection monitor */
	Detection monitor;

	/* stack depth for StackTrace.capture */
	int default_stack_depth;
	/* stack matching depth for StackTrace hash_value */
	int default_match_depth;

private:
	/* global flag */

	volatile bool enabled;

	/* bypasses template instantiation checks */
	bool disable_template_instance;

	/* global avoidance lock */
	pthread_mutex_t avoidLock;

	/* allowed map */
	AllowedMap allowed;
	void remove_allowed(Thread* t, Mutex* m, Position* p);
	void insert_allowed(Thread* t, Mutex* m, Position* p);

	/* yielders */
	Yielders yielders;

	/* map[position] = { templates that contain position } */
	PositionTemplatesMap history_positions;

	/* cache all positions found so far */
//	pthread_mutex_t positionLock;
	ULock plock;
	PositionCacheMap position_cache;

	/* total yields done */
	uint64_t yield_count;

	bool yield_request(Thread* thr, Mutex* mtx, Position* pos);
	void template_instance(Thread* thr, Mutex* mtx, Position* pos);
	bool match_template(TemplateInstance &ti);

	/* pthread_cleanup function */
	static void thread_cleanup(void* arg);

	Position* getPos(int id);//for microbenchmark
};

}; // namespace dlock

#endif

