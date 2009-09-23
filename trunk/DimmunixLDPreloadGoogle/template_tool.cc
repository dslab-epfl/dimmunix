#include "position.h"
#include "template.h"
#include <iostream>
#include <fstream>

using namespace std;
using namespace dlock;


void show_position(Position &p) {
	cout << "\tposition" << endl;
	for (int i = 0; i < p.trace.stack.size(); ++i)
		cout << "\t\t" << p.trace.stack[i] << endl;
}

void show_template(Template &t) {
	cout << "Template" << endl;
	for_each(t.positions.begin(), t.positions.end(), show_position);
}

int main(int argc, char** argv) {

	vector<Template> templates;

	ifstream ifile;
	ifile.open(argv[1], ifstream::in | ifstream::binary);
	if (ifile.is_open()) {
		size_t n;
		ifile.read((char*)&n, sizeof(size_t));
		while ((n-- > 0) && ifile.good()) {
			Template tmpl;
			ifile >> tmpl;
			templates.push_back(tmpl);
		}
		ifile.close();
	}


	for_each(templates.begin(), templates.end(), show_template);

	return 0;
}
