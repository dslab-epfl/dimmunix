#include "position.h"
#include <iostream>
#include <fstream>


void foo() {
	dlock::Position p1;
	p1.capture();
	p1.print_unlocked();

	ofstream ofile;
	ofile.open("position.pos", fstream::out | fstream::binary);
	ofile << p1;
	ofile.close();
}

void bar() {
	foo();
}

int main() {
	bar();

	dlock::Position p2;
	ifstream ifile;
	ifile.open("position.pos", fstream::in | fstream::binary);
	ifile >> p2;
	ifile.close();

	p2.print_unlocked();

	return 0;
}
