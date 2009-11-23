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


#include <pthread.h>
#include <unistd.h>
#include <stdio.h>

pthread_mutex_t mutex1;
pthread_mutex_t mutex2;

void f1() {
	pthread_mutex_lock(&mutex1);
	sleep(1);
	pthread_mutex_lock(&mutex2);
	pthread_mutex_unlock(&mutex2);
	pthread_mutex_unlock(&mutex1);
	printf("thread 1 done\n");
}

void g1() {
	pthread_mutex_lock(&mutex2);
	sleep(1);
	pthread_mutex_lock(&mutex1);
	pthread_mutex_unlock(&mutex1);
	pthread_mutex_unlock(&mutex2);
	printf("thread 2 done\n");
}

void* f(void* args) {
	f1();
}

void* g(void* args) {
	g1();
}

int main() {

	pthread_mutex_init(&mutex1, 0);
	pthread_mutex_init(&mutex2, 0);

	pthread_t t1;
	pthread_t t2;

	pthread_create(&t1, NULL, f, NULL);
	pthread_create(&t2, NULL, g, NULL);

	pthread_join(t1, NULL);
	pthread_join(t2, NULL);
}
