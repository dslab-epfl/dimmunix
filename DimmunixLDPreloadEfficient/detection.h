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
#include "uthash.h"
#include "template_instance.h"
#include <map>
#include "hash.h"

//#include <tr1/unordered_map>
//using namespace __gnu_cxx;

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
	//typedef unordered_map<Thread*, EventQueue*, hash<Thread*> > EventQueueThreadMap;
	typedef unordered_map<Thread*, vector<Event>, hash<Thread*> > ThreadEventsMap;

	struct eq_thread_item {
		int64_t key; /* Thread* */
		EventQueue* q;
		UT_hash_handle hh;
	};


	Detection();
	~Detection();

	void register_thread(Thread* _t) {
		eq_lock.lock();
//		event_queues[_t] = &_t->_eq;

		struct eq_thread_item *item = (struct eq_thread_item*) malloc(sizeof(struct eq_thread_item));
		item->key = (int64_t)_t;
		item->q = &_t->_eq;
		HASH_ADD_INT(event_queues, key, item);

		eq_lock.unlock();
	}

	void add_observer(DetectionObserver* _obs) { observers.push_back(_obs); }

//	void addInstance(const TemplateInstance& ti) {
//		eq_lock.lock();
//		currentInstances_av.push_back(ti);
//		eq_lock.unlock();
//	}

	vector<Template>* history_av;

	volatile bool enabled;

private:
//	pthread_mutex_t eventsLock; /* event queue lock */
	ULock eq_lock;
	set<Thread*> req_threads; /* RequestingThreads */
	vector<Template> history;
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

//	EventQueueThreadMap event_queues;
	struct eq_thread_item* event_queues;

//	ThreadEventsMap sync_ops;

	/* monitor thread attributes and methods */
	pthread_t monitor_tid;
	volatile bool monitor_run;
	int sleep_time;

	void process_events();
	void process_sync_events();
	void process_event(Event& event);
	void check_for_cycles();

	TemplateInstance getAvoidedInstance(const Event& relEvt);
	bool isTruePositive(const TemplateInstance& ti, const Event& relEvt);
	void compactifySyncTraces();
	long getMinTimestamp();

	static void* monitor_thread(void* arg);

//	vector<TemplateInstance> currentInstances_av;
//	vector<TemplateInstance> currentInstances;

};


}; // namespace dlock

#endif
