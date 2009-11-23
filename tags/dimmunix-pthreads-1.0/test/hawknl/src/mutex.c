/*
  HawkNL mutex module
  Copyright (C) 2000-2002 Phil Frisbie, Jr. (phil@hawksoft.com)

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Library General Public
  License as published by the Free Software Foundation; either
  version 2 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Library General Public License for more details.

  You should have received a copy of the GNU Library General Public
  License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place - Suite 330,
  Boston, MA  02111-1307, USA.

  Or go to http://www.gnu.org/copyleft/lgpl.html
*/

#include "nlinternal.h"

#ifdef NL_WIN_THREADS
struct nl_mutex_t
{
  CRITICAL_SECTION mutex;
};
#else
#include <pthread.h>
struct nl_mutex_t
{
  pthread_mutex_t   mutex;
};
#endif /* NL_WIN_THREADS */

NL_EXP NLboolean NL_APIENTRY nlMutexInit(NLmutex *mutex)
{
	if(mutex == NULL)
	{
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
	}
    else
    {
        NLmutex mx;
#ifdef NL_WIN_THREADS
        /* native Windows */

        mx = (NLmutex)malloc(sizeof(struct nl_mutex_t));
	    if(mx == NULL)
	    {
		    nlSetError(NL_OUT_OF_MEMORY);
            return NL_FALSE;
	    }
        InitializeCriticalSection(&mx->mutex);
#else
        /* POSIX */
        int         result;

        mx = (NLmutex)malloc(sizeof(struct nl_mutex_t));
	    if(mx == NULL)
	    {
		    nlSetError(NL_OUT_OF_MEMORY);
            return NL_FALSE;
	    }
        result = pthread_mutex_init((pthread_mutex_t *)&mx->mutex, NULL);
        if(result != 0)
        {
#ifdef WINDOWS_APP
            SetLastError(result);
#endif
		    nlSetError(NL_SYSTEM_ERROR);
            return NL_FALSE;
        }
#endif
        *mutex = mx;
    }
    return NL_TRUE;
}

NL_EXP NLboolean NL_APIENTRY nlMutexLock(NLmutex *mutex){
  
  if(mutex == NULL){
    nlSetError(NL_NULL_POINTER);
    return NL_FALSE;
  }
  
  if(*mutex == NULL) {
    nlSetError(NL_NULL_POINTER);
    return NL_FALSE;
  
  }else{
    NLmutex mx = *mutex;
#ifdef NL_WIN_THREADS
    /* native Windows */
    EnterCriticalSection(&mx->mutex);
#else
    int result;
    
    /* POSIX */
    result = pthread_mutex_lock((pthread_mutex_t *)&mx->mutex);
    if(result != 0){
#ifdef WINDOWS_APP
      SetLastError(result);
#endif
      nlSetError(NL_SYSTEM_ERROR);
      return NL_FALSE;
    }
#endif
  }
}

NL_EXP NLboolean NL_APIENTRY nlMutexLock2(NLmutex *mutex){
  NLmutex mx = *mutex;
  int result = pthread_mutex_lock((pthread_mutex_t *)&mx->mutex);
}

NL_EXP NLboolean NL_APIENTRY nlMutexUnlock(NLmutex *mutex)
{
	if(mutex == NULL)
	{
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
	}
	if(*mutex == NULL)
	{
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
	}
    else
    {
        NLmutex mx = *mutex;
#ifdef NL_WIN_THREADS
        /* native Windows */
        LeaveCriticalSection(&mx->mutex);
#else
        int result;

        /* POSIX */
        result = pthread_mutex_unlock((pthread_mutex_t *)&mx->mutex);
        if(result != 0)
        {
#ifdef WINDOWS_APP
            SetLastError(result);
#endif
		    nlSetError(NL_SYSTEM_ERROR);
            return NL_FALSE;
        }
#endif
    }
}

NL_EXP NLboolean NL_APIENTRY nlMutexDestroy(NLmutex *mutex)
{
	if(mutex == NULL)
	{
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
	}
	if(*mutex == NULL)
	{
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
	}
    else
    {
        NLmutex mx = *mutex;
#ifdef NL_WIN_THREADS
        /* native Windows */
        DeleteCriticalSection(&mx->mutex);
#else
        /* POSIX */
        (void)pthread_mutex_destroy((pthread_mutex_t *)&mx->mutex);
#endif
    }
    free(*mutex);
    *mutex = NULL;
    return NL_TRUE;
}
