#ifndef _DLOCK_HASH_H
#define _DLOCK_HASH_H

#include <utility>
#include <vector>

#include <stdint.h>


/* taken from TR1 and Boost hash.hpp */

using namespace std;

namespace dlock {

inline size_t hash_value(void* v) { return (intptr_t)v; }
inline size_t hash_value(unsigned int v) { return v; }

template<typename T>
struct hash : public unary_function<T, size_t> {
	inline size_t operator()(T const &v) const { return hash_value(v); }
};

template<typename T>
inline void hash_combine(size_t & seed, T const& v) {
	seed ^= hash_value(v) + 0x9e3779b9 + (seed << 6) + (seed >> 2);
}

template<typename T>
inline size_t hash_range(T first, T last) {
	size_t seed = 0;
	for (; first != last; ++first)
		hash_combine(seed, *first);
	return seed;
}

template<typename A, typename B>
inline size_t hash_value(pair<A, B> const &v) {
	size_t seed = 0;
	hash_combine(seed, v.first);
	hash_combine(seed, v.second);
	return seed;
}

template<typename T, typename A>
inline size_t hash_value(vector<T, A> const &v) {
	return hash_range(v.begin(), v.end());
}

}; // namespace

#endif

