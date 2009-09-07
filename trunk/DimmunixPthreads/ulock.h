#ifndef __ULOCK_H
#define __ULOCK_H

#include <pthread.h>

typedef unsigned long ulong;

inline unsigned char CAS(volatile ulong* _addr, ulong _old, ulong _new) {
	unsigned char rslt;
	__asm __volatile("lock; cmpxchg %3, %0; setz %1"
					: "=m"(*_addr), "=q"(rslt)
					: "m"(*_addr), "r"(_new), "a"(_old)
					: "memory");
	return rslt;
}

#define CPU_PAUSE __asm __volatile ("pause")

class ULock {
public:
	ULock() : owner(0), lcount(0) {}

	void lock() {
		ulong tid = (ulong)pthread_self();
		if (tid != owner) {
			if (CAS(&owner, 0, tid) == 0) {
				for (;;) {
					CPU_PAUSE;
					if (owner == 0) {
						if (CAS(&owner, 0, tid) != 0)
							goto done;
					}
				}
			}
		}
	done:
		++lcount;
	}

	void unlock() {
		if (--lcount == 0)
			owner = 0;
	}

private:
	volatile ulong owner;
	ulong lcount;
};

#endif
