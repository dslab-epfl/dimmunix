#include <signal.h>
#include <unistd.h>
#include <time.h>
#include <sys/time.h>

#include "detection.h"
#include "dlock.h"

#include <algorithm>
#include <iostream>
#include <fstream>
using namespace std;


namespace dlock {

#define DEFAULT_SLEEP_TIME 100
#define DOT_FNAME "dlock.cycle"

Detection::Detection() : enabled(true) {
	TemplateFactory::load_templates(history, HISTORY_FNAME);
//	pthread_mutex_init(&eventsLock, 0);
//	dlock_mutex_disable_avoidance(&eventsLock);
	pthread_create(&monitor_tid, 0, Detection::monitor_thread, this); /* make sure "this" is valid */
}

Detection::~Detection() {
	monitor_run = false;
	pthread_cancel(monitor_tid); /* kills detection monitor */
	pthread_join(monitor_tid, 0); /* waits here */
//	pthread_mutex_destroy(&eventsLock);
}

void Detection::process_event(Event& event) {
	switch (event.type) {
	case Event::REQUEST:
		rag.request(event.t, event.m, event.p);
		req_threads.insert(event.t);
		break;

	case Event::YIELD:
		rag.yield(event.t, event.t->yield_cause);
		break;

	case Event::GRANT:
		rag.request_to_grant(event.t, event.m, event.p);
		break;

	case Event::ACQUIRED:
		rag.grant_to_hold(event.t, event.m, event.p);
		if (!req_threads.empty())
			req_threads.erase(event.t);
		break;

	case Event::RELEASE:
		rag.release(event.m, event.t);
		break;

	case Event::CANCEL:
		rag.cancel(event.t, event.m);
		if (!req_threads.empty())
			req_threads.erase(event.t);
		break;
	case Event::JOIN:
	case Event::JOIN_DONE:
		/* TODO */
		break;
	}
}

void Detection::process_events() {
	pthread_testcancel();

	if (!enabled)
		return;

	Event event, req_event;
	vector<Event> ev_vec;
	bool in_seq = false;
	foreach(EventQueueThreadMap::iterator, it_eq, event_queues) {
		size_t size = (*it_eq).second->length(); /* get queue length */
		while ( (size) && (*it_eq).second->dequeue(event) ) {
			--size;
			if (!in_seq) {
				if (event.type == Event::REQUEST) {
					/* we save the request to compare its thread/mutex */
					in_seq = true;
					req_event = event;
					ev_vec.push_back(event);
				} else
					process_event(event);
			} else {
				if (req_event.m == event.m) {
					/* same mutex as previous request */
					if (event.type == Event::RELEASE) {
						in_seq = false;
						ev_vec.clear(); /* discard this sequence of events because is just a lock/unlock */
					} else
						ev_vec.push_back(event);
				} else {
					/* thread or mutex are different, process ev_vec normally and current event */
					in_seq = false;
					foreach(vector<Event>::iterator, it_ev, ev_vec)
						process_event(*it_ev);
					ev_vec.clear();
					process_event(event);
				}
			}
		}
		/* process any pending event */
		if (in_seq) {
			foreach(vector<Event>::iterator, it_ev, ev_vec)
				process_event(*it_ev);
			ev_vec.clear();
			in_seq = false;
		}
	}

	/* check for cycles */
	check_for_cycles();
}

void Detection::check_for_cycles() {
	int ithr;
	vector<Edge> cycle;
	if (rag.has_cycle(req_threads, cycle)) {
		DLOCK_DEBUGF("deadlock found\n");
		enabled = false; /* disable detection */
		notify_deadlock(); /* notify listeners */
#ifndef NDEBUG
		/* dumps the RAG in graphviz format */
		ofstream ofile;
		ofile.open(DOT_FNAME, fstream::out | fstream::binary);
		rag.dump_dot(ofile);
		ofile.close();
#endif
		Template t;
		foreach_r(vector<Edge>::reverse_iterator, it, cycle)
			if ((*it).type == Edge::HOLD)
				t.positions.push_back(*(*it).pos); /* add hold edge positions */
		history.push_back(t);
		TemplateFactory::save_templates(history, HISTORY_FNAME); /* save history */

		monitor_run = false; /* stop monitor thread */
		raise(abort_signal); /* signal the process */
	} else if ((ithr = rag.has_allcycles(req_threads, cycle) >= 0)) {
		DLOCK_DEBUGF("livelock found\n");
		notify_livelock(NULL); /* TODO pass the correct thread */

		Edge* first_y = NULL;
		Template t;
		foreach_r(vector<Edge>::reverse_iterator, it, cycle) {
			if ( (first_y == NULL) && (*it).type == Edge::YIELD && (*it).source->edges.size() == 1 )
				first_y = &(*it); // get the first unique yield edge, less risky
			if ((*it).type == Edge::HOLD || (*it).type == Edge::YIELD)
				t.positions.push_back(*(*it).pos); // add hold and yield positions
		}
		history.push_back(t); /* save new livelock template */

		if (first_y == NULL) {
			DLOCK_DEBUGF("no single outgoing edge found (damn), lets get the first pure yield node\n");
			foreach_r(vector<Edge>::reverse_iterator, it, cycle) {
				if ( (*it).type == Edge::YIELD && (*it).source->all_yields() ) {
					first_y = &(*it);
					break;
				}
			}
		}

		if (first_y == NULL) { // ... (damn^2)
			DLOCK_DEBUGF("BOK! the thread doesn't contain any yield edge to break... lets go berserk!");
			foreach_r(vector<Edge>::reverse_iterator, it, cycle) {
				if ( (*it).type == Edge::YIELD ) {
					first_y = &(*it);
					break;
				}
			}
		}

		if (first_y == NULL) {
			DLOCK_DEBUGF("livelock found without yield edge ?!\n");
			return;
		}

		Thread* thr_y = (Thread*)first_y->source->value; // hack!!!
		thr_y->bypass_avoidance = true;
		thr_y->yield_notify(); /* break yield */
	}
}

void* Detection::monitor_thread(void* arg) {
	Detection* det = (Detection*)arg;

	sigset_t sset;
	sigfillset(&sset);
	pthread_sigmask(SIG_UNBLOCK, &sset, 0);

	char* tmp = 0;
	if ((tmp = getenv("DIMMU_MONITOR_SLEEP")) != NULL) {
		det->sleep_time = atoi(tmp);
		if (det->sleep_time < 0) {
			det->enabled = false; /* disables monitoring */
			return NULL; /* exit monitor thread */
		}
	} else
		det->sleep_time = DEFAULT_SLEEP_TIME;

	det->monitor_run = true;

	det->abort_signal = SIGTERM;
	if ((tmp = getenv("DIMMU_SIGNAL")) != NULL)
		det->abort_signal = atoi(tmp);

	struct timespec ts;
	while (det->monitor_run) {
		ts.tv_sec = det->sleep_time / 1000;
		ts.tv_nsec = (det->sleep_time % 1000)  * 1000000;
		nanosleep(&ts, NULL);
		det->process_events();
	}
	return NULL;
}


}; // namespace dlock

