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

