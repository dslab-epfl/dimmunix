/*
  HawkNL time module
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

#if defined (_WIN32_WCE)
#ifdef _MSC_VER
#pragma warning (disable:4201)
#pragma warning (disable:4214)
#endif /* _MSC_VER */

#include <winbase.h>

#ifdef _MSC_VER
#pragma warning (default:4201)
#pragma warning (default:4214)
#endif /* _MSC_VER */

#include "nlinternal.h"

struct timeb {
    time_t time;
    unsigned short millitm;
};

static void ftime( struct timeb *tb )
{
    SYSTEMTIME  st;
    int days, years, leapyears;

    if(tb == NULL)
    {
		nlSetError(NL_NULL_POINTER);
		return;
    }
    GetSystemTime(&st);
    leapyears = (st.wYear - 1970 + 1) / 4;
    years = st.wYear - 1970 - leapyears;

    days = years * 365 + leapyears * 366;

    switch (st.wMonth) {
    case 1:
    case 3:
    case 5:
    case 7:
    case 8:
    case 10:
    case 12:
        days += 31;
        break;
    case 4:
    case 6:
    case 9:
    case 11:
        days += 30;
        break;
    case 2:
        days += (st.wYear%4 == 0) ? 29 : 28;
        break;
    default:
        break;
    }
    days += st.wDay;
    tb->time = days * 86400 + st.wHour * 3600 + st.wMinute * 60 + st.wSecond;
    tb->millitm = st.wMilliseconds;
}

#else
#include "nlinternal.h"
#include <sys/timeb.h>
#endif

NL_EXP NLboolean NL_APIENTRY nlTime(NLtime *t)
{
#ifdef WINDOWS_APP
    static NLboolean        needinit = NL_TRUE;
    static NLboolean        haspcounter = NL_FALSE;
    static LARGE_INTEGER    freq;
    static LARGE_INTEGER    lastcount;
    static NLtime           currenttime;

    if(t == NULL)
    {
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
    }
    if(needinit == NL_TRUE)
    {
        QueryPerformanceFrequency(&freq);
        if(freq.QuadPart != 0)
        {
            if(QueryPerformanceCounter(&lastcount) != 0)
            {
                /* get the current time */
                struct timeb tb;

                ftime(&tb);
                currenttime.seconds = tb.time;
                currenttime.useconds = tb.millitm * 1000;
                haspcounter = NL_TRUE;
            }
        }
        needinit = NL_FALSE;
    }
    if(haspcounter == NL_TRUE)
    {
        LARGE_INTEGER   currentcount;
        LARGE_INTEGER   diffcount;

        QueryPerformanceCounter(&currentcount);
        diffcount.QuadPart = currentcount.QuadPart - lastcount.QuadPart;
        lastcount.QuadPart = currentcount.QuadPart;
        while(diffcount.QuadPart >= freq.QuadPart)
        {
            diffcount.QuadPart -= freq.QuadPart;
            currenttime.seconds++;
        }
        currenttime.useconds += (NLlong)(diffcount.QuadPart * 1000000 / freq.QuadPart);
        if(currenttime.useconds >= 1000000)
        {
            currenttime.useconds -= 1000000;
            currenttime.seconds++;
        }
        t->seconds = currenttime.seconds;
        t->mseconds = currenttime.useconds / 1000;
        t->useconds = currenttime.useconds;
    }
    else
    {
        /* fall back to ftime */
        struct timeb tb;

        ftime(&tb);
        t->seconds = tb.time;
        t->mseconds = tb.millitm;
        t->useconds = tb.millitm * 1000;
    }
#else /* !WINDOWS_APP */
    struct timeval tv;

    if(t == NULL)
    {
		nlSetError(NL_NULL_POINTER);
		return NL_FALSE;
    }
    gettimeofday(&tv, NULL);
    t->seconds = tv.tv_sec;
    t->mseconds = tv.tv_usec / 1000;
    t->useconds = tv.tv_usec;
#endif /* !WINDOWS_APP */
    return NL_TRUE;
}

/* Windows CE does not have time.h functions */
#if defined (_WIN32_WCE)

time_t time(time_t *timer)
{
    NLtime t;

    nlTime(&t);
    *timer = t.seconds;

    return *timer;
}

#endif
