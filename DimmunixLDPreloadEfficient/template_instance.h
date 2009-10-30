#ifndef __TEMPLATE_INSTANCE_H
#define __TEMPLATE_INSTANCE_H

/*
 *  template_instance.h
 *
 *
 *  Created by Daniel Tralamazza on 4/15/08.
 *  Copyright 2008 . All rights reserved.
 *
 */

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
