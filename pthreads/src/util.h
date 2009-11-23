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

#ifndef __DLOCK_UTIL_H
#define __DLOCK_UTIL_H

#include <stdlib.h>
#include <sys/cdefs.h>
#include <time.h>
#include <sys/time.h>

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

long currentTimeUsec();

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
