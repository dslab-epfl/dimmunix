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
