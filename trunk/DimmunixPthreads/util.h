#ifndef __DLOCK_UTIL_H
#define __DLOCK_UTIL_H

#include <stdlib.h>
#include <sys/cdefs.h>

/* atomic stuff */
#ifdef SMP
#define BUSLOCK "lock; "
#else
#define BUSLOCK
#endif
#define ATOMIC_INC(v) __asm__ __volatile__(BUSLOCK "inc %0" : "=m"(v) : "m"(v))

// less portable (gcc only) but really cool foreach
#define auto_foreach(i,c)\
	typedef __typeof__(c) c##_CONTAINERTYPE;\
	for( c##_CONTAINERTYPE::iterator i = c.begin(); i != c.end(); ++i )

#ifndef foreach
#define foreach(type,i,ctr) for (type i = (ctr).begin(); i != (ctr).end(); ++i)
#endif

#define foreach_r(type,i,ctr) for (type i = (ctr).rbegin(); i != (ctr).rend(); ++i)

#ifdef __cplusplus
extern "C" {
#endif

/* yes we need our own printf_unlocked */
void printf_nonblocking(const char* /*fmt*/, ...);

#ifdef __cplusplus
}
#endif

/* debug printf macro */
#ifdef NDEBUG
#define DLOCK_DEBUGF(ftm,...)
#else
#define DLOCK_DEBUGF(fmt,...) printf_nonblocking("[%s@%d] " fmt, __func__, __LINE__, ##__VA_ARGS__)
#endif

#endif
