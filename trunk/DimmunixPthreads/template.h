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
	Template() {}
	Template(const Template &t) : positions(t.positions) {}

	Template& operator = (const Template &t) {
		if (this != &t)
			positions = t.positions;
		return *this;
	}

	void print();

	void clear() { positions.clear(); }

	vector<Position> positions;
};


class TemplateFactory {
public:
	static void load_templates(vector<Template> &_templs, const char* fname);
	static void save_templates(vector<Template> &_templs, const char* fname);
};

}; // namespace dlock

#endif

