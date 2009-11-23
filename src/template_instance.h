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

#ifndef __TEMPLATE_INSTANCE_H
#define __TEMPLATE_INSTANCE_H

#include <vector>
#include "thread.h"
#include "mutex.h"
#include "template.h"
#include "position.h"


namespace dlock {

class TemplateInstance {
public:
	TemplateInstance() : yieldee(NULL, NULL, NULL, 0) { }

	TemplateInstance(Template *tmpl, Thread* thr, Mutex* mtx, Position* pos, long time) : yieldee(pos, thr, mtx, time) {
		bool removed = false; /* a template may contain duplicate positions, we remove only one */
		foreach(vector<Position>::iterator, it, tmpl->positions) {
			if ((*it).hashcode != pos->hashcode || removed)
				tupples.push_back(YieldCause(&(*it), NULL, NULL, 0));
			else if (!removed)
				removed = true;
		}
		unmatched = tupples.size();
		this->tmpl = tmpl;
	}

	void match() { --unmatched; }

	bool all_match() const { return unmatched == 0; }

	unsigned int unmatched;

	vector<YieldCause> tupples;

	YieldCause yieldee;
	Template* tmpl;

	inline bool isNULL() {
		return yieldee.thread == NULL;
	}

	vector<Mutex*> peerLocks;
};


}; // namespace

#endif
