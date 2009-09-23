#include "position.h"
#include "util.h"
#include <dlfcn.h>


namespace dlock {

void StackTrace::print_unlocked() {
	Dl_info info;
	printf_nonblocking("\tstack depth %u\n", stack.size());
	printf_nonblocking("\tmatch depth %u\n", match_depth);
	for (uint32_t i = 0; i < stack.size(); ++i)
		printf_nonblocking("\t[%u] %p %s\n", i, stack[i], (dladdr(stack[i], &info) == 0) ? "(not found)" : info.dli_sname);
}

static void* lookup_symbol(const char* symb) {
	static void* dl_handle = dlopen(0, RTLD_LAZY);
	return dlsym(dl_handle, symb);
}

ostream& operator << (ostream& os, const StackTrace& st) {
	os << st.stack.size() << ' ' << st.match_depth << std::endl;
	Dl_info info;
	if (st.stack.size() > 0)
		for (size_t i = 0; i < st.stack.size(); ++i) {
			if (dladdr(st.stack[i], &info))
				os << info.dli_sname << ' ' << (uintptr_t)st.stack[i] - (uintptr_t)info.dli_saddr;
			else
				os << "_ 0";
			os << ' ' << (uintptr_t)st.stack[i] << std::endl;
		}
	return os;
}

istream& operator >> (istream& is, StackTrace& st) {
	size_t newsize;
	is >> newsize >> st.match_depth;
	st.stack.resize(newsize);
	if (newsize > 0) {
		string s;
		void* s_addr;
		uintptr_t orig_addr;
		size_t off;
		for (size_t i = 0; i < newsize; ++i) {
			is >> s >> off >> orig_addr;
			if (s != "_")
				s_addr = (void*)lookup_symbol(s.c_str());
			else
				s_addr = 0;
			if (s_addr)
				st.stack[i] = (void*)((uintptr_t)s_addr + off);
			else
				st.stack[i] = (void*)orig_addr;
		}
	}
	return is;
}

ostream& operator << (ostream& os, const Position& p) {
	os << p.hashcode << ' ' << p.hashcode_full << ' ' << p.trace;
	return os;
}

istream& operator >> (istream& is, Position& p) {
	is >> p.hashcode >> p.hashcode_full >> p.trace;
	Position::hash_t tmphash = hash_value(p.trace);
	if (tmphash != p.hashcode)
		printf_nonblocking("[warn] hashcode mismatch: read %u but computed %u\n", p.hashcode, tmphash);
	return is;
}

void Position::print_unlocked() {
	printf_nonblocking("[#%u]\n", hashcode);
	trace.print_unlocked();
}

};

