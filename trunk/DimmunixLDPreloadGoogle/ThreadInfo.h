#ifndef THREADINFO_H_
#define THREADINFO_H_

#include <ext/hash_map>
#include <vector>
#include <pthread.h>
#include "dlock.h"

using namespace __gnu_cxx;
using namespace std;

class ThreadInfo {
public:
	volatile bool inDimmunix;

	dlock_thread_t* dt;

	ThreadInfo();

};

#endif /*THREADINFO_H_*/
