#include "position.h"
#include "util.h"
#include <dlfcn.h>


namespace dlock {

void StackTrace::print_unlocked() const {
	Dl_info info;
	printf_nonblocking("\tstack depth %u\n", stack.size());
	printf_nonblocking("\tmatch depth %u\n", match_depth);
	for (unsigned int i = 0; i < stack.size(); ++i)
		printf_nonblocking("\t[%u] %p %s\n", i, stack[i], (dladdr(stack[i], &info) == 0) ? "(not found)" : info.dli_sname);
}

size_t StackTrace::offset_hash() const {
	vector<uintptr_t> offsets;
	Dl_info info;
	for (size_t i = 0; i < stack.size(); ++i)
		if (dladdr(stack[i], &info))
			offsets.push_back((uintptr_t)stack[i] - (uintptr_t)info.dli_fbase);
		else
			offsets.push_back((uintptr_t)stack[i]);
	return hash_range(offsets.begin(), offsets.end());
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
			//if (dladdr(st.stack[i], &info) && info.dli_saddr != 0) /* saddr can be 0x0 even if returns true */
			if (dladdr(st.stack[i], &info))
				os << info.dli_fname << ' ' << (uintptr_t)st.stack[i] - (uintptr_t)info.dli_fbase;
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
//			s_addr = (s == "_") ? 0 : (void*)lookup_symbol(s.c_str());
			s_addr = (s == "_") ? 0 : dlopen(s.c_str(), RTLD_LAZY);
			st.stack[i] = (s_addr) ? (void*)((uintptr_t)s_addr + off) : (void*)orig_addr;
		}
	}
	return is;
}

ostream& operator << (ostream& os, const Position& p) {
	size_t offhash = p.trace.offset_hash();
	os << p.hashcode << ' ' << offhash << ' ' << p.trace;
	return os;
}

istream& operator >> (istream& is, Position& p) {
	size_t orig_offhash;
	is >> p.hashcode >> orig_offhash >> p.trace;
//	size_t tmp_offhash = p.trace.offset_hash();
//	if (tmp_offhash != orig_offhash)
//		printf_nonblocking("[warn] hashcode mismatch: read %u but computed %u\n", orig_offhash, tmp_offhash);
	return is;
}

void Position::print_unlocked() const {
	printf_nonblocking("[#%u]\n", hashcode);
	trace.print_unlocked();
}

};

