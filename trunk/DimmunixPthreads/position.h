#ifndef __DLOCK_POSITION_H
#define __DLOCK_POSITION_H

#include <iostream>
#include <stdint.h>

#ifdef HAS_LIBUNWIND
#define UNW_LOCAL_ONY
#include <libunwind.h>
static int backtrace(void** stack, int d) {
	unw_cursor_t cursor; unw_context_t uc;
	unw_word_t ip, sp;
	int r = 0
	unw_getcontext(&uc);
	unw_init_local(&cursor, &uc);
	while ((unw_step(&cursor) > 0) && (d-- > 0)) {
		unw_get_reg(&cursor, UNW_REG_IP, &ip);
		stack[r++] = ip;
	}
	return r;
}
#else
#include <execinfo.h>
#endif

#include "hash.h"
#include "util.h"

using namespace std;

/* adjusting this value impacts directly on template matching */
#ifndef DIMMU_MAX_STACK_DEPTH
#define DIMMU_MAX_STACK_DEPTH 15
#endif

#ifndef DIMMU_STACK_SKIP_START
#define DIMMU_STACK_SKIP_START 2
#endif

namespace dlock {


class StackTrace {
	friend ostream& operator<< (ostream& os, const StackTrace& st);
	friend istream& operator>> (istream& is, StackTrace& st);
public:
	/* default ctor */
	StackTrace() : match_depth(DIMMU_MAX_STACK_DEPTH) {}

	/* copy ctor */
	StackTrace(const StackTrace &st) : stack(st.stack), match_depth(st.match_depth) {}

	/* assign operator */
	StackTrace& operator = (const StackTrace& st) {
		if (this != &st) {
			stack = st.stack;
			match_depth = st.match_depth;
		}
		return *this;
	}

	void capture(const int depth = DIMMU_MAX_STACK_DEPTH) {
		stack.resize(depth);
		int n = backtrace(&stack[0], depth);
		if (depth != n) /* no need to resize again */
			stack.resize(n);
		if (match_depth > n)
			match_depth = n; /* match_depth should always be < or = stack.size */
	}

	void print_unlocked();

	/* vector of addresses */
	vector<void*> stack;

	/* hash value is computed until this depth */
	int match_depth;
};

inline size_t hash_value(StackTrace const &st) {
	return hash_range(st.stack.begin(), st.stack.begin() + st.match_depth);
}

class Position {
	friend ostream& operator<< (ostream& os, const Position& p);
	friend istream& operator>> (istream& is, Position& p);

public:
	typedef size_t hash_t;

	/* creates a position from a stacktrace */
	Position(StackTrace &st) : trace(st) {
		hashcode = hash_value(st);
	}

	/* creates an empty position */
	Position() : hashcode(0) {}

	/* copy ctor */
	Position(const Position &p) : trace(p.trace), hashcode(p.hashcode) {}

	/* assign operator */
	Position& operator = (const Position& p) {
		if (this != &p) {
			hashcode = p.hashcode;
			trace = p.trace;
		}
		return *this;
	}

	/* recompute the hashcode and update match_depth */
	void rehash(size_t _depth) {
		if (_depth <= trace.stack.size()) {
			trace.match_depth = _depth;
			hashcode = hash_value(trace);
		}
	}

	bool operator == (const Position &p) const { return hashcode == p.hashcode; }

	void print_unlocked();

	StackTrace trace;
	Position::hash_t hashcode;
};

inline size_t hash_value(Position const &v) {
	return v.hashcode;
}

}; // namespace dlock


#endif

