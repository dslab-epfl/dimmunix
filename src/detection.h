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
