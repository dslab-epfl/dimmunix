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
	TemplateInstance(Template *tmpl, Thread* thr, Mutex* mtx, Position* pos) : fp(false), yieldee(pos, thr, mtx) {
		bool removed = false;
		foreach(vector<Position>::iterator, it, tmpl->positions) {
			if ((*it).hashcode != pos->hashcode || removed)
				fTupples.push_back(YieldCause(&(*it), NULL, NULL));
			else if (!removed) {
				/* same hashcode, now check for full match */
				if ((*it).partial_match(pos))
					fp = true;
				removed = true;
			}
		}
		unmatched = fTupples.size();
	}

	bool match(Position* p, Thread* t, Mutex* mtx) {
		if (t == yieldee.thread)
			return true; /* this is a hack so we ignore the yieldee outside */
		if (mtx == yieldee.mutex)
			return false;
		foreach(vector<YieldCause>::iterator, it, fTupples) {
			if ( ((*it).pos->hashcode == p->hashcode) && ((*it).thread == NULL) ) {
				(*it).thread = t;
				(*it).mutex = mtx;
				--unmatched;
				/* same hashcode, now check for full match */
				if (!fp && (*it).pos->partial_match(p))
					fp = true;
				return true;
			}
		}
		return false;
	}

	bool false_positive() const { return fp; }
	bool all_match() const { return unmatched == 0; }
	vector<YieldCause>& tupples() { return fTupples; };

private:
	unsigned int unmatched;
	bool fp;
	vector<YieldCause> fTupples;
	YieldCause yieldee;
};


}; // namespace

#endif

