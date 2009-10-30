#ifndef __DLOCK_TEMPLATE_H
#define __DLOCK_TEMPLATE_H


#include <stdint.h>
#include <iostream>
#include <vector>
#include "position.h"

using namespace std;

namespace dlock {

class Template {
	friend ostream& operator<< (ostream& os, const Template& tmpl);
	friend istream& operator>> (istream& is, Template& tmpl);

public:
	Template(): nFPs(0), nTPs(0), nInst(0), enabled(true), id(0) {}
	Template(const Template &t) : positions(t.positions), nFPs(t.nFPs), nTPs(t.nTPs), nInst(t.nInst), enabled(t.enabled), id(t.id) {}

	Template& operator = (const Template &t) {
		if (this != &t) {
			positions = t.positions;
			nTPs = t.nTPs;
			nFPs = t.nFPs;
			nInst = t.nInst;
			enabled = t.enabled;
			id = t.id;
		}
		return *this;
	}

	void print();

	void clear() { positions.clear(); }

	vector<Position> positions;

	volatile unsigned nFPs;
	volatile unsigned nTPs;
	volatile unsigned nInst;
	volatile bool enabled;
	int id;
};


class TemplateFactory {
public:
	static void load_templates(vector<Template> &_templs, const char* fname);
	static void save_templates(vector<Template> &_templs, const char* fname);
};

}; // namespace dlock

#endif

