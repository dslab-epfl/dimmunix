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
	TemplateInstance(Template *tmpl, Thread* thr, Mutex* mtx, Position* pos) : yieldee(pos, thr, mtx) {
		bool removed = false; /* a template may contain duplicate positions, we remove only one */
		foreach(vector<Position>::iterator, it, tmpl->positions) {
			if ((*it).hashcode != pos->hashcode || removed)
				tupples.push_back(YieldCause(&(*it), NULL, NULL));
			else if (!removed)
				removed = true;
		}
		unmatched = tupples.size();
	}

	void match() { --unmatched; }

	bool all_match() const { return unmatched == 0; }

	unsigned int unmatched;

	vector<YieldCause> tupples;

	YieldCause yieldee;
};


}; // namespace

#endif
