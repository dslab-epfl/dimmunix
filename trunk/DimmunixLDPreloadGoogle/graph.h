#ifndef __DLOCK_GRAPH_H
#define __DLOCK_GRAPH_H

/*
 * graph.h
 *
 * Ressource Allocation Graph class
 *
 * Created by Daniel Tralamazza & Gizil Oguz on 4/4/08.
 * Copyright 2008. All rights reserved.
 */


#include <iostream>
#include <map>
#include <set>
#include <vector>

#include <ext/hash_map>
using namespace __gnu_cxx;

#include <stdint.h>
#include "mutex.h"
#include "position.h"
#include "thread.h"

namespace dlock {

class Edge;

class Vertex {
public:
	typedef Vertex& Reference;
	typedef Vertex* Ptr;
	typedef vector<Edge> Edges;
	enum Color { WHITE = 0, GREY, BLACK, RED };
	enum Type { THREAD = 0, LOCK };

	Vertex() : value(0), timestamp(0), type(Vertex::THREAD), color(Vertex::WHITE), kafa(false) {}

	Vertex(const Vertex &v) : value(v.value), timestamp(v.timestamp), type(v.type), color(v.color), kafa(v.kafa), open_cycles(v.open_cycles) {}

	Vertex& operator = (const Vertex &v) {
		if (this != &v) {
			value = v.value;
			timestamp = v.timestamp;
			type = v.type;
			color = v.color;
			edges = v.edges;
			kafa = v.kafa;
			open_cycles = v.open_cycles;
		}
		return *this;
	}

	bool operator == (const Vertex &v) const { return value == v.value; }
	bool operator < (const Vertex &v) const { return value < v.value; }

	bool all_yields();

	intptr_t value; /* (int)ptr to a thread/lock */
	int timestamp;
	Vertex::Type type;
	Vertex::Color color;
	Vertex::Edges edges;
	bool kafa;
	set<intptr_t> open_cycles;
};

class Edge {
public:
	enum Type { REQUEST = 0, YIELD, GRANT, HOLD, JOIN };

	Edge(Vertex::Ptr src, Vertex::Ptr tgt, Edge::Type _t, Position* p) : source(src), target(tgt), type(_t), pos(p) {}

	Edge(const Edge& e) : source(e.source), target(e.target), type(e.type), pos(e.pos) {}

	Edge& operator=(const Edge& e) {
		if (this != &e) {
			source = e.source;
			target = e.target;
			type = e.type;
			pos = e.pos;
		}
		return *this;
	}

	Vertex::Ptr source;
	Vertex::Ptr target;
	Edge::Type type;
	Position* pos;
};

class Graph {
	//typedef map<intptr_t, Vertex> VertexMap;
	typedef hash_map<intptr_t, Vertex> VertexMap;
public:
	void request(Thread* t, Mutex* m, Position* p);
	void request_to_grant(Thread* t, Mutex* m, Position* p);
	void grant_to_hold(Thread* t, Mutex* m, Position* p);
	void release(Mutex* m, Thread* t);
	void cancel(Thread* t, Mutex* m);
	void yield(Thread* t, vector<YieldCause> yc); // pass a copy of yield_cause

	/* scan ALL vertexes in the graph for cycles*/
	bool has_cycle(vector<Edge> &cycle);
	/* scan the graph for cycles starting by vertex t */
	bool has_cycle(Thread* t, vector<Edge> &cycle);
	/* scan the graph for cycles containing threads in vt */
	bool has_cycle(set<Thread*> &vt, vector<Edge> &cycle);

	/* scan the graph and returns true if any thread t in vt is livelocked */
	int has_allcycles(set<Thread*> &vt, vector<Edge> &cycle);

	void dump(ostream&);
	/* save to graphviz format*/
	void dump_dot(ostream&);

private:
	/* return 0 no cycle, 1 cycle, 2 cycle finished */
	int dfs_visit(Vertex &v, vector<Edge> &cycle);

	int dfs_visit_all(Vertex &v, vector<Edge> &cycle);

	int visit_kafas(Vertex &v, vector<Vertex::Ptr> &kafas);
	int find_only_cycles(Vertex &v, vector<Edge> &cycle);
	/* global timestamp */
	int timestamp;
	VertexMap graph;
};

}; // namespace dlock

#endif
