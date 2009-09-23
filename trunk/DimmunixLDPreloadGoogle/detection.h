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

#include <ext/hash_map>
using namespace __gnu_cxx;

using namespace std;

namespace dlock {

#define HISTORY_FNAME "dlock.history"

class Event {
public:
	enum Type { REQUEST, YIELD, GRANT, ACQUIRED, RELEASE, CANCEL, JOIN, JOIN_DONE };

	Event(Position* _p, Thread* _t, Mutex* _m, Type _type) : p(_p), t(_t), m(_m), type(_type) {}

	Position* p;
	Thread* t;
	Mutex* m;
	Event::Type type;
};


/* Observer patttern */
struct DetectionObserver {
	virtual void on_deadlock() = 0;
	virtual void on_livelock(Thread* thr) = 0;
};


class Detection {
public:
	typedef vector<Event> EventQueue;

	Detection();
	~Detection();

	void register_event_queue(Thread* _t, EventQueue& eq) { event_queues[_t] = eq; }

	void push_event(Position* _p, Thread* _t, Mutex* _m, Event::Type _type);

	void add_observer(DetectionObserver* _obs) { observers.push_back(_obs); }

private:
	pthread_mutex_t eventsLock; /* event queue lock */
	EventQueue events; /* event queue */
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

	hash_map<Thread*, EventQueue, hash<Thread*> > event_queues;

	/* monitor thread attributes and methods */
	pthread_t monitor_tid;
	volatile bool monitor_run;
	int sleep_time;

	void process_events();

	static void* monitor_thread(void* arg);
};


}; // namespace dlock

#endif
