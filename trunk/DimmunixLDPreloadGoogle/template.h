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
	Template() : instantiations(0), false_instantiations(0) {}

	Template(const Template &t) : positions(t.positions), instantiations(t.instantiations), false_instantiations(t.false_instantiations)  {}

	Template& operator = (const Template &t) {
		if (this != &t) {
			positions = t.positions;
			instantiations = t.instantiations;
			false_instantiations = t.false_instantiations;
		}
		return *this;
	}

	void instantiated(bool fp) {
		if (fp)
			++false_instantiations;
		else
			++instantiations;
	}

	void print();

	void clear() {
		positions.clear();
		instantiations = 0;
		false_instantiations = 0;
	}

	vector<Position> positions;
	unsigned int instantiations;
	unsigned int false_instantiations;
};


class TemplateFactory {
public:
	static void load_templates(vector<Template> &_templs, const char* fname);
	static void save_templates(vector<Template> &_templs, const char* fname);
};

}; // namespace dlock

#endif

