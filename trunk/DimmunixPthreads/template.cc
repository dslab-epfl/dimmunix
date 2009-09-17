#include <algorithm>
#include <fstream>
#include "template.h"
#include "util.h"


namespace dlock {

ostream& operator << (ostream& os, const Template& tmpl) {
	os << tmpl.positions.size() << std::endl;
	foreach(vector<Position>::const_iterator, it, tmpl.positions)
		os << *it;
	return os;
}

istream& operator >> (istream& is, Template& tmpl) {
	size_t size;
	is >> size;
	Position p;
	for (size_t i = 0; i < size; ++i) {
		is >> p;
		tmpl.positions.push_back(p);
	}
	return is;
}

void Template::print() {
	foreach(vector<Position>::iterator, it, positions) {
		(*it).print_unlocked();
		printf_nonblocking("\n");
	}
}

void TemplateFactory::save_templates(vector<Template> &templs, const char* fname) {
	size_t n = templs.size();
	if (n <= 0)
		return;
	printf("deadlock!\n");
	ofstream ofile;
	ofile.open(fname, ofstream::out | ofstream::trunc);
	ofile << templs.size() << std::endl;
	foreach(vector<Template>::iterator, it, templs) {
		ofile << *it;
	}
	ofile.close();
}

void TemplateFactory::load_templates(vector<Template> &templs, const char* fname) {
	ifstream ifile;
	ifile.open(fname);//, ifstream::in | ifstream::binary);
	if (!ifile.is_open())
		return;
	size_t n;
	ifile >> n;
	Template tmpl;
	while ((n > 0) && ifile.good()) {
		ifile >> tmpl;
		templs.push_back(tmpl);
		--n;
	}
	ifile.close();
	if (n > 0)
		DLOCK_DEBUGF("could not load all templates (%u) from %s\n", n, fname);
}

};

