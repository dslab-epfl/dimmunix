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

#include "graph.h"
#include "util.h"

namespace dlock {

/* vertexes are identified by memory location */
#define VHASH(obj) ((intptr_t)obj)

bool Vertex::all_yields() {
	foreach(Vertex::Edges::iterator, it, edges)
		if ((*it).type != Edge::YIELD)
			return false;
	return true;
}

void Graph::request(Thread* t, Mutex* m, Position* p) {
	Vertex::Ptr vthr = &graph[VHASH(t)];
	vthr->value = VHASH(t);
	Vertex::Ptr vmtx = &graph[VHASH(m)];
	vmtx->value = VHASH(m);
	vmtx->type = Vertex::LOCK;
	vthr->edges.push_back(Edge(vthr, vmtx, Edge::REQUEST, p));
}

void Graph::request_to_grant(Thread* t, Mutex* m, Position* p) {
	Vertex::Ptr vthr = &graph[VHASH(t)]; /* edges from Thread t */
	foreach(Vertex::Edges::iterator, it_e, vthr->edges) {
		/* find the one pointing to Mutex m */
		if ((*it_e).target->value == VHASH(m)) {
			(*it_e).type = Edge::GRANT; /* change its type */
			(*it_e).pos = p; /* update position */
			break;
		}
	}
}

/* check if */
void Graph::grant_to_hold(Thread* t, Mutex* m, Position* p) {
	Vertex::Ptr vthr = &graph[VHASH(t)];
	Vertex::Ptr vmtx = &graph[VHASH(m)];
	bool found = false;
	/* edges from Thread t */
	foreach(Vertex::Edges::iterator, it_e, vthr->edges) {
		/* find the one pointing to Mutex m */
		if ((*it_e).target->value == vmtx->value) {
			vthr->edges.erase(it_e); /* remove it */
			/* add edge from Mutex m to Thread t */
			vmtx->edges.push_back(Edge(vmtx, vthr, Edge::HOLD, p));
			found = true;
			break;
		}
	}
	if (!found) /* this is a trylock or timedlock hold (no previous request) */
		vmtx->edges.push_back(Edge(vmtx, vthr, Edge::HOLD, p));
}

void Graph::release(Mutex* m, Thread* t) {
	/* edges from Mutex m */
	Vertex &vmtx = graph[VHASH(m)];
	foreach(Vertex::Edges::iterator, it_e, vmtx.edges) {
		/* find the one pointing to Thread t */
		if ((*it_e).target->value == VHASH(t)) {
			vmtx.edges.erase(it_e); /* remove it */
			break;
		}
	}
}

/* TODO */
void Graph::cancel(Thread* t, Mutex* m) {
/*	Vertex &vthr = graph[VHASH(t)];
	foreach(Vertex::Edges::iterator, it_e, vthr.edges) {
		if ((*it_e).target->value == VHASH(m)) {
			vthr.edges.erase(it_e);
			break;
		}
	}*/
}

void Graph::yield(Thread* t, vector<YieldCause>& yc) {
	Vertex::Ptr vthr = &graph[VHASH(t)];
	for (Vertex::Edges::iterator it_e = vthr->edges.begin(); it_e != vthr->edges.end(); ) {
		if ((*it_e).type == Edge::YIELD)
			it_e = vthr->edges.erase(it_e);
		else
			++it_e;
	}
	foreach(vector<YieldCause>::iterator, it, yc) {
		Vertex::Ptr vthr_y = &graph[VHASH((*it).thread)];
		vthr->edges.push_back(Edge(vthr, vthr_y, Edge::YIELD, (*it).pos));
	}
}

void Graph::dump(ostream &out) {
	foreach(VertexMap::iterator, it, graph) {
		out << "[" << (*it).first << "]" << endl;
		foreach(Vertex::Edges::iterator, it_edge, (*it).second.edges)
			out << "\t" << (*it_edge).target->value << endl;
	}
}

void Graph::dump_dot(ostream& out) {
	static const char* dot_colors[3] = { "lightgrey", "red", "green" };
	static const char* dot_shape[2] = { "box", "ellipse" };
	out << "digraph G {" << endl;
	foreach(VertexMap::iterator, it, graph)
		out << "\tnode [color=" << dot_colors[(*it).second.color] << ",shape=" << dot_shape[(*it).second.type] << ",label=\"" << (*it).first << " (" << (*it).second.timestamp << "/" << timestamp << ")\"]; " << (*it).first << endl;
	foreach(VertexMap::iterator, it, graph)
		foreach(Vertex::Edges::iterator, it_edge, (*it).second.edges)
			out << "\t" << (*it).first << "->" << (*it_edge).target->value << ";" << endl;
	out << "}" << endl;
}

bool Graph::has_cycle(Thread* t, vector<Edge> &cycle) {
	foreach(VertexMap::iterator, it, graph)
		(*it).second.color = Vertex::WHITE;
	timestamp = 0;
	return dfs_visit(graph[VHASH(t)], cycle) > 0; /* this should not return 1 */
}

bool Graph::has_cycle(vector<Edge> &cycle) {
	foreach(VertexMap::iterator, it, graph)
		(*it).second.color = Vertex::WHITE;
	timestamp = 0;
	foreach(VertexMap::iterator, it, graph) {
		Vertex &v = (*it).second;
		if (v.color == Vertex::WHITE)
			if (dfs_visit(v, cycle) > 0) /* this should not return 1 */
				return true;
	}
	return false;
}

bool Graph::has_cycle(set<Thread*> &vt, vector<Edge> &cycle) {
	foreach(VertexMap::iterator, it, graph)
		(*it).second.color = Vertex::WHITE;
	timestamp = 0;
	foreach(set<Thread*>::iterator, it, vt) {
		Vertex &v = graph[VHASH((*it))];
		if (v.color == Vertex::WHITE)
			if (dfs_visit(v, cycle) > 0 && cycle.size() > 2) /* this should not return 1 */
				return true;
	}
	return false;
}

int Graph::has_allcycles(set<Thread*> &vt, vector<Edge> &cycle) {
	foreach(VertexMap::iterator, it, graph)
		(*it).second.color = Vertex::WHITE;
	timestamp = 0;
	int r = 0;
	int i = 0;
	vector<Vertex::Ptr> kafas;
	foreach(set<Thread*>::iterator, it, vt) {
		Vertex &v = graph[VHASH((*it))];
		kafas.clear();
		r = visit_kafas(v, kafas);
		if (r == 0) {
			foreach(Vertex::Edges::iterator, it_edge, cycle)
				(*it_edge).target->color = Vertex::BLACK;
			cycle.clear();
		} else {
			foreach(VertexMap::iterator, it, graph) {
				(*it).second.color = Vertex::WHITE;
				(*it).second.open_cycles.clear();
			}
			foreach(vector<Vertex::Ptr>::iterator, it_k, kafas)
				(*it_k)->kafa = false;
			foreach(vector<Vertex::Ptr>::iterator, it_k, kafas)
				find_only_cycles(*(*it_k), cycle);
			break;
		}
		++i;
	}
	if (r == 0)
		return -1;
	else
		return i;
/*	foreach(VertexMap::iterator, it, graph)
		(*it).second.color = Vertex::WHITE;
	timestamp = 0;
	int r = 0;
	foreach(vector<Thread*>::iterator, it, vt) {
		Vertex &v = graph[VHASH((*it))];
		r = dfs_visit_all(v, cycle);
		if (r == 0) {
			foreach(Vertex::Edges::iterator, it_edge, cycle)
				(*it_edge).target->color = Vertex::BLACK;
			cycle.clear();
		} else
			break;
	}
	return r == 1;*/
}

int Graph::visit_kafas(Vertex &v, vector<Vertex::Ptr> &kafas) {
	if (v.color == Vertex::BLACK)
		return 0; // safe
	if (v.color == Vertex::RED)
		return 1; // cycle
	if (v.color == Vertex::GREY) {
		if (!v.kafa) {
			v.kafa = true;
			kafas.push_back(&v);
		}
		return 2; // start of a cycle
	}

	v.color = Vertex::GREY;
	v.timestamp = ++timestamp;
	int result = 0;
	foreach(Vertex::Edges::iterator, it_edge, v.edges) {
		Vertex &tgt = *(*it_edge).target;
		result = visit_kafas(tgt, kafas);
		if (result == 0)
			break; // safe child found, we can stop
	}
	++timestamp;
	if (result == 0)
		v.color = Vertex::BLACK;
	else
		v.color = Vertex::RED;
	return result;
}

int Graph::find_only_cycles(Vertex &v, vector<Edge> &cycle) {
	if (v.color == Vertex::GREY) {
		if (!v.kafa) {
			v.kafa = true;
			v.open_cycles.insert(v.value);
		}
		return 2; // start of a cycle
	}
	// When this returns RED it means someone with 2 or more outgoings edges is pointing to us
	// We should decide if we should add this edge when we backtrack
	if (v.color == Vertex::RED)
		return 1; // cycle
	v.color = Vertex::GREY;
	v.timestamp = ++timestamp;
	int result = 0;
	foreach(Vertex::Edges::iterator, it_edge, v.edges) {
		Vertex &tgt = *(*it_edge).target;
		result = find_only_cycles(tgt, cycle);
		v.open_cycles.insert(tgt.open_cycles.begin(), tgt.open_cycles.end());
		if (!tgt.open_cycles.empty())
			cycle.push_back(*it_edge);
		if (v.kafa) {
			v.kafa = false;
		//TODO bokafa, what if kafa is not inside!?
			v.open_cycles.erase(v.value);
		}
	}
	++timestamp;
	v.color = Vertex::RED;
	return result;
}

int Graph::dfs_visit_all(Vertex &v, vector<Edge> &cycle) {
	if (v.color == Vertex::BLACK)
		return 0; // safe because from here on there is at least one terminal node
	if ((v.color == Vertex::GREY) || (v.color == Vertex::RED))
		return 1; //cycle
	v.color = Vertex::GREY; // now visiting
	v.timestamp = ++timestamp;
	int result = 0;
	foreach(Vertex::Edges::iterator, it_edge, v.edges) {
		Vertex &tgt = *(*it_edge).target;
		result = dfs_visit_all(tgt, cycle);
		if (result == 0)
			break; // we have a safe child, we can stop and backtrack
		else
			cycle.push_back(*it_edge);
	}
	++timestamp;
	if (result == 0)
		v.color = Vertex::BLACK;
	else
		v.color = Vertex::RED;
	return result;
}

int Graph::dfs_visit(Vertex &v, vector<Edge> &cycle) {
	v.color = Vertex::GREY;
	v.timestamp = ++timestamp;
	foreach(Vertex::Edges::iterator, it_edge, v.edges) {
		if ((*it_edge).type == Edge::YIELD)
			continue;
		Vertex &tgt = *(*it_edge).target;
		if (tgt.color == Vertex::WHITE) {
			int r = dfs_visit(tgt, cycle);
			switch(r) {
			/* case 0 doesn't exist because we don't return */
			case 1:
				if (tgt == *cycle.front().target)
					r = 2; /* we reached the end of the cycle stop adding nodes */
				else
					cycle.push_back(*it_edge);
			case 2:
				return r;
			}
		} else if (tgt.color == Vertex::GREY) {
			cycle.push_back(*it_edge);
			return 1; // back edge cycle found
		}
	}
	v.color = Vertex::BLACK;
	++timestamp;
	return 0;
}

}; //namespace dlock
