#ifndef __DLOCK_EVENT_H
#define __DLOCK_EVENT_H

#include "thread.h"
#include "mutex.h"
#include "position.h"
#include "spsc_queue.h"

namespace dlock {

class Event {
public:
	enum Type { REQUEST, YIELD, GRANT, ACQUIRED, RELEASE, CANCEL, JOIN, JOIN_DONE };

	Event() {}
	Event(Position* _p, Thread* _t, Mutex* _m, Type _type) : p(_p), t(_t), m(_m), type(_type) {}

	Position* p;
	Thread* t;
	Mutex* m;
	Event::Type type;
};

typedef spsc_queue<Event> EventQueue;

}; // namespace dlock

#endif
