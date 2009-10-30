#include "util.h"
#include <stdarg.h>
#include <stdio.h>
#include <stdint.h>


static void puts_nonblocking(const char* s) {
	if (s) while (putchar_unlocked(*s++)) ;
}

static void putnumber(unsigned int n, int base) {
	char buf[33];
	char* p = &buf[32];
	*p = '\0';
	do {
		*--p = "01234567890abcdef"[n % base];
	} while (n /= base);
	puts_nonblocking(p);
}

/* unlocked version of (a simple) printf */
void printf_nonblocking(const char* fmt, ...) {
	va_list ap;
	char *s;
	int d;
	unsigned int u;

	va_start(ap, fmt);
	while (*fmt != '\0') {
		if (*fmt != '%') {
			putchar_unlocked(*fmt++);
			continue;
		}
		switch(*++fmt) {
			case 's':
				s = va_arg(ap, char *);
				puts_nonblocking(s);
				break;
			case 'd':
				d = va_arg(ap, int);
				if (d < 0) { putchar_unlocked('-'); d = -d; }
				putnumber(d, 10);
				break;
			case 'u':
				u = va_arg(ap, unsigned int);
				putnumber(u, 10);
				break;
			case 'p':
				u = (unsigned int)va_arg(ap, void*);
				putchar_unlocked('0');
				putchar_unlocked('x');
				putnumber(u, 16);
				break;
			default:
				putchar_unlocked(*fmt);
				break;
		}
		fmt++;
	}
	va_end(ap);
}

long currentTimeUsec() {
	struct timeval t;
	gettimeofday(&t, NULL);
	return t.tv_sec* 1000* 1000+ t.tv_usec;
}


