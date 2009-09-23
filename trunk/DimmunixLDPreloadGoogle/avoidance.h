#ifndef __DLOCK_AVOIDANCE_H
#define __DLOCK_AVOIDANCE_H

#include <ext/hash_map>
#include <map>
#include <vector>

#include <pthread.h>

#include "detection.h"
#include "thread.h"
#include "mutex.h"
#include "position.h"
#include "template.h"
#include "template_instance.h"
#include "hash.h"


#define SAVE_YIELD_POS 1

using namespace __gnu_cxx;

namespace dlock {

class Avoidance : public DetectionObserver {
	typedef hash_map<Position::hash_t, Position*, hash<Position::hash_t> > PositionCacheMap;
	typedef hash_map<Position::hash_t, vector<Template*>, hash<Position::hash_t> > PositionTemplatesMap;
	typedef hash_map<YieldCause, vector<Thread*>, hash<YieldCause> > Yielders;
public:
	Avoidance();
	~Avoidance();

	void acquire(Thread* thr, Mutex* mtx, int non_blocking);
	void acquired(Thread* thr, Mutex* mtx);
	void release(Thread* thr, Mutex* mtx);
	void cancel(Thread* thr, Mutex* mtx);

	Thread* create_thread();
	void remove_thread(Thread* t);

	/* notification from DetectionObserver */
	void on_deadlock() { enabled = false; }
	void on_livelock(Thread* t) {}

private:
	/* global flag */
	volatile bool enabled;
	/* bypasses template instantiation checks */
	bool disable_template_instance;
	/* detection monitor */
	Detection* monitor;
	/* global avoidance lock */
	pthread_mutex_t avoidLock;

	/* threads vector lock */
	pthread_mutex_t threadsLock;
	/* all threads */
	vector<Thread*> threads;

//	hash_map<Position::hash_t, vector<Thread*>, hash<Position::hash_t> > lockGrantees;

	/* yielders */
	Yielders yielders;
	/* template history */
	vector<Template> history;
	/* map[position] = { templates that contain position } */
	PositionTemplatesMap history_positions;

	/* cache all positions found so far */
	pthread_mutex_t positionLock;
	PositionCacheMap position_cache;

	/* stack depth for StackTrace.capture */
	int default_stack_depth;
	/* stack matching depth for StackTrace hash_value */
	int default_match_depth;

	/* total yields done */
	uint64_t yield_count;

	bool yield_request(Thread* thr, Mutex* mtx, Position* pos);
	void template_instance(Thread* thr, Mutex* mtx, Position* pos);
	bool match_template(TemplateInstance &ti);

	/* pthread_cleanup function */
	static void thread_cleanup(void* arg);
};

}; // namespace dlock

#endif
