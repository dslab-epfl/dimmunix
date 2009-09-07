#ifndef __DLOCK_DETECTION_H
#define __DLOCK_DETECTION_H

#include <pthread.h>
#include <queue>
#include <set>
#include <vector>
#include "thread.h"
#include "mutex.h"
#include "position.h"
#include "template.h"
#include "graph.h"
#include "util.h"
#include "event.h"
#include "ulock.h"

#include <tr1/unordered_map>
using namespace __gnu_cxx;

using namespace std;

namespace dlock {

#define HISTORY_FNAME "dlock.history"

/* Observer patttern */
struct DetectionObserver {
	virtual void on_deadlock() = 0;
	virtual void on_livelock(Thread* thr) = 0;
};


class Detection {
public:
	//typedef spsc_queue<Event> EventQueue;
	typedef unordered_map<Thread*, EventQueue*, hash<Thread*> > EventQueueThreadMap;

	Detection();
	~Detection();

	void register_thread(Thread* _t) {
//		pthread_mutex_lock(&eventsLock);
		eq_lock.lock();
		event_queues[_t] = &_t->_eq;
//		pthread_mutex_unlock(&eventsLock);
		eq_lock.unlock();
	}

	void unregister_thread(Thread* _t) {
//		pthread_mutex_lock(&eventsLock);
		eq_lock.lock();
		event_queues.erase(_t);
//		pthread_mutex_unlock(&eventsLock);
		eq_lock.unlock();
	}

	void add_observer(DetectionObserver* _obs) { observers.push_back(_obs); }

private:
//	pthread_mutex_t eventsLock; /* event queue lock */
	ULock eq_lock;
	set<Thread*> req_threads; /* RequestingThreads */
	vector<Template> history;
	volatile bool enabled;
	int abort_signal;
	Graph rag;
	vector<DetectionObserver*> observers;

	void notify_deadlock() {
		auto_foreach(it, observers)
			(*it)->on_deadlock();
	}

	void notify_livelock(Thread* thr) {
		auto_foreach(it, observers)
			(*it)->on_livelock(thr);
	}

	EventQueueThreadMap event_queues;

	/* monitor thread attributes and methods */
	pthread_t monitor_tid;
	volatile bool monitor_run;
	int sleep_time;

	void process_events();
	void process_event(Event& event);
	void check_for_cycles();

	static void* monitor_thread(void* arg);
};


}; // namespace dlock

#endif
