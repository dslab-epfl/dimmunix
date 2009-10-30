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

	Event() { }
	Event(Position* _p, Thread* _t, Mutex* _m, Type _type, long _time, const vector<YieldCause>& _yield_cause) : p(_p), t(_t), m(_m), type(_type), time(_time), yield_cause(_yield_cause) {}

	Position* p;
	Thread* t;
	Mutex* m;
	Event::Type type;
	long time;
	vector<YieldCause> yield_cause;
};

typedef spsc_queue<Event> EventQueue;

}; // namespace dlock

#endif
