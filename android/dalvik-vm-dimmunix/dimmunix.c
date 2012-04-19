#include "Dalvik.h"
#include <sched.h>
#include <sys/stat.h>

struct Queue* positions;
struct Template* history;
int histSize;
pthread_mutex_t avoidanceLock;
struct Cycle cycle;
struct Cycle stack;
struct Node* joinPointFound;
int blocked;
volatile int enabled = false;

pthread_t monitorThread;
unsigned int nSyncs;
unsigned int nSyncThreads;
unsigned int nYields;

void initDimmunix() {
	histSize = 0;
	init_Cycle(&cycle);
	init_Cycle(&stack);
	joinPointFound = NULL;
	blocked = false;

	nSyncs = 0;
	nSyncThreads = 0;
	nYields = 0;

	pthread_mutex_init(&avoidanceLock, NULL);

    if((positions=(struct Queue*)malloc_zero(sizeof(struct Queue)*MAXPOSITIONS))==NULL){
        LOGD("ERROR ALLOCATING DIMMUNIX positions");
    }
    if((history=(struct Template*)malloc_zero(sizeof(struct Template)*MAXTEMPLATES))==NULL){
        LOGD("ERROR ALLOCATING DIMMUNIX history");
    }

    int i;
    for (i = 0; i < MAXPOSITIONS; i++) {
    	initQueue(&positions[i]);
    }

	loadHistory();

//	pthread_create(&monitorThread, NULL, printStats, NULL);

	enabled = true;

	LOGD("initialized Dimmunix");
}

void* printStats(void* args) {
	unsigned int lastSyncsPerSec = 0;
	unsigned int lastYieldsPerSec = 0;

	while (1) {

		usleep(STATS_PERIOD_SEC* 1000* 1000);

		pthread_mutex_lock(&avoidanceLock);
		unsigned int syncsPerSec = nSyncs/ STATS_PERIOD_SEC;
		unsigned int yieldsPerSec = nYields/ STATS_PERIOD_SEC;
		unsigned int nthr = nSyncThreads;
		pthread_mutex_unlock(&avoidanceLock);

		LOGD("PID %d --- %u threads performed %u syncs/sec, %u yields/sec", getpid(), nthr, syncsPerSec- lastSyncsPerSec, yieldsPerSec- lastYieldsPerSec);
		lastSyncsPerSec = syncsPerSec;
		lastYieldsPerSec = yieldsPerSec;
	}
}

void stopDimmunix() {
	LOGD("shut down %d", getpid());
//    free(positions);
//    free(history);
}

void initNode(struct Node* _node, void* _id, int _type) {
	_node->next = NULL;
	_node->posNext = NULL;
	_node->owner = NULL;
	_node->acqPos = NULL;
	_node->nLockings = 0;
	_node->id = _id;
	_node->color = 0;
	_node->type = _type;

	_node->nYields = 0;
	if (_type == 0) {
		_node->yields = (struct Node**)malloc(MAXTEMPLATESIZE* sizeof(struct Node*));
		_node->posYields = (struct Position**)malloc(MAXTEMPLATESIZE* sizeof(struct Position*));
	}
	else {
		_node->yields = NULL;
		_node->posYields = NULL;
	}
}

void init_Position(struct Position* _p, char* _pos) {
	initQueue(&_p->lockGrantees);
	initQueue(&_p->removedGrantees);

	int n = strlen(_pos)+ 1;
	_p->id = (char*)malloc((n+ 1)* sizeof(char));
    strcpy(_p->id, _pos);

    _p->inHist = false;
}

void init_Template(struct Template* _t) {
	_t->size = 0;
	pthread_cond_init(&_t->avoidanceCondVar, NULL);
}

void init_Cycle(struct Cycle* _c) {
	_c->len = 0;
}
//avoidance
/*
static void loadHistory() {
}*/

void requestEvent(struct Node* _t, struct Node* _l, struct Position* _p) {
	_t->next = _l;
	_t->posNext = _p;
}

void grantEvent(struct Node* _t, struct Node* _l,struct Position* _p) {
	_t->nYields = 0;
}

void addYieldEdge(struct Node* _n, struct Node* _t, struct Position* _p) {
	_n->yields[_n->nYields] = _t;
	_n->posYields[_n->nYields] = _p;
	_n->nYields = _n->nYields + 1;
}

void yieldEvent(struct Node* _t, struct Template* _tmpl) {
	_t->nYields = 0;

	int i = 0;
	while (i < _tmpl->size) {
		if (_tmpl->threads[i] != _t) {
			addYieldEdge(_t, _tmpl->threads[i], _tmpl->positions[i]);
		}
		i = i + 1;
	}
}

void acquiredEvent(struct Node* _t, struct Node* _l, struct Position* _p) {
	_l->next = _t;
	_l->posNext = _p;
	_t->next = NULL;
	_t->posNext = NULL;
}

void releaseEvent(struct Node* t, struct Node* l) {
	l->next = NULL;
	l->posNext = NULL;
}

//avoidance
int threadIsNew(struct Template* templ, int n, struct Node* t) {
	int i = 0;
	while (i < n) {
		if (templ->threads[i] == t)
			return false;
		i = i + 1;
	}
	return true;
}

//avoidance
int instance(struct Template* templ, int n)	{
	if (n == templ->size) {
		return true;
	}
    struct Position* p = templ->positions[n];

	struct QueueElem* lg = p->lockGrantees.head;
	while (lg != NULL) {
		struct Node* tnode = (struct Node*)lg->elem;

		if (threadIsNew(templ, n, tnode)) {
			templ->threads[n] = tnode;

			if (instance(templ, n + 1)) {
				return true;
			} else {
				templ->threads[n] = NULL;
			}
		}

		lg = lg->next;
	}

	return false;
}

void add(struct Cycle* _c, struct Node* _n, struct Position* _p) {
	_c->nodes[_c->len] = _n;
	_c->labels[_c->len] = _p;
	_c->len = _c->len + 1;
}

void backtrack(struct Cycle* _c)	{
	_c->nodes[_c->len - 1] = NULL;
	_c->labels[_c->len - 1] = NULL;
	_c->len = _c->len - 1;
}

int hasCycles(struct Node* _node) {
	blocked = false;
	_node->color = 1;

	if (_node->next == NULL) {
		_node->color = 2;
		return false;
    }

	if (!hasCycleNext(_node)) {
		if (!hasCycleYields(_node)) {
			return false;
		}
	}
	return true;
}

int hasCycleNext(struct Node* _n) {
	if (_n->type == 1) {
		add(&stack, _n, _n->posNext);
	} else {
		add(&stack, _n, NULL);
	}

	if ((_n->next)->color == 1) {
		if (joinPointFound == NULL) {
			copy(&stack, &cycle);
			joinPointFound = (_n->next);
		}
		blocked = true;
	} else if ((_n->next)->color == 0) {
		blocked = hasCycles(_n->next);
	}
	backtrack(&stack);
	if (blocked) {
		return true;
	} else {
		joinPointFound = NULL;
		return false;
	}
}

int hasCycleYields(struct Node* _n) {
	if (_n->nYields == 0) {
		_n->color = 2;
		return false;
	}
	int i = 0;
	while (i < _n->nYields) {
		add(&stack, _n, _n->posYields[i]);
		blocked = false;
		if ((_n->yields[i])->color == 1) {
			if (joinPointFound == NULL) {
				copy(&stack, &cycle);
				joinPointFound = (_n->yields[i]);
			}
			blocked = true;
		} else if ((_n->yields[i])->color == 0) {
			blocked = hasCycles(_n->yields[i]);
		}
		backtrack(&stack);

		if (!blocked) {
			_n->color = 2;
			joinPointFound = NULL;
			return false;
		}
		i = i + 1;
	}
	return true;
}

void copy(struct Cycle* _cIn, struct Cycle* _cOut) {
	_cOut->len = _cIn->len;

	int i = 0;
    	while (i < _cIn->len) {
		_cOut->nodes[i] = _cIn->nodes[i];
		_cOut->labels[i] = _cIn->labels[i];
		i = i + 1;
	}
}

void Template_add(struct Template* _tmpl, struct Position* _p) {
	_tmpl->positions[_tmpl->size] = _p;
	_tmpl->size = _tmpl->size + 1;
}

void getTemplate(struct Template* _tmpl)	{

	int i = 0;
	while (cycle.nodes[i] != joinPointFound)
      	i = i + 1;

	//int j = i;
	while (i < cycle.len) {
		if (cycle.labels[i] != NULL) {
			Template_add(_tmpl, cycle.labels[i]);
		}
		i = i + 1;
    }
}

int traversed(struct Node* x) {
	int i;
	for (i = 0; i < stack.len; i++) {
		if (stack.nodes[i] == x)
			return true;
	}
	return false;
}

void resetColors(struct Node* x) {
	if (x == NULL)
		return;

	if (traversed(x)) {
		return;
	}
	add(&stack, x, NULL);

	x->color = 0;

	resetColors(x->next);

	int i;
	for (i = 0; i < x->nYields; i++) {
		resetColors(x->yields[i]);
	}
}

void reset(struct Node* x) {
	stack.len = 0;
	cycle.len = 0;
	joinPointFound = NULL;

	resetColors(x);

	stack.len = 0;
}

int Pos_equals(struct Position * _position, char* _stack) {

    int posLength = strlen(_position->id);
    int stackLength = strlen(_stack);
    if (stackLength != posLength){
       return false;
    }

	int i = 0;
	while (i < posLength) {
		if (_position->id[i] != _stack[i]){
            return false;
        }
		i = i + 1;
	}

	return true;

}

int filterPos(char* _stack) {
	if (strncmp(_stack, "java.", 5) == 0)
		return 1;
	return 0;
}

struct Position* getPosition(char* _stack) {

    int hash = hashOfStack(_stack);

    struct Queue* q = &positions[hash];

    struct Position* pos = findInQueuePos(q, _stack);
    if(pos == NULL){
        pos = (struct Position*)malloc_zero(sizeof(struct Position));
        init_Position(pos, _stack);
        addToQueue(q, pos);
    }
    //LOGD("pos = %s", pos->id);

    return pos;
}

int checkForCycles(struct Node* _t) {
	reset(_t);
	if (hasCycles(_t)) {
		LOGD("deadlock found !");
		struct Template tmpl;
		init_Template(&tmpl);
		getTemplate(&tmpl);
		saveTemplate(&tmpl);
		return true;
	}
	return false;
}

void toString(struct Template* _tmpl, char* _output){
    int i = 0;
    _output[0] = 0;
    while(i<(_tmpl->size)){
        if(i>0){
            strcat(_output, ";");
        }
        strcat(_output, (_tmpl->positions[i])->id);
        i=i+1;
    }
}

void loadHistory() {
	mkdir("/data/anr", 0777);

	FILE* hist = fopen(DIMHISTFILE, "r");
	if (hist == NULL) {
		LOGD("empty deadlock history");
		return;
	}

	LOGD("loading deadlock history");
	char buf[4* STACKSIZE+ 1];
	while (fscanf(hist, "%s", buf) != EOF) {
		struct Template sig;
		char* tok = strtok(buf, ";");
		int i = 0;
		while (tok != NULL) {
			sig.positions[i] = getPosition(tok);
			sig.positions[i]->inHist = true;
			tok = strtok(NULL, ";");
			i++;
		}
		sig.size = i;
		addToHistory(&sig);
		toString(&sig, buf);
		LOGD(buf);
	}
	fclose(hist);
}

void saveTemplate(struct Template* _tmpl) {
	if(addToHistory(_tmpl) == true){
		LOGD("saving new deadlock signature");
		char sig[4*STACKSIZE+1];
		toString(_tmpl, sig);
		LOGD(sig);

		FILE *hist;
		hist = fopen(DIMHISTFILE, "a");
		if (hist==NULL) {
			LOGD("Can't open history file");
		}
		else {
			fprintf(hist, "%s\n", sig);
			fclose(hist);
		}
	}
}

void grant(struct Position* _p, struct Node* _t) {
	struct QueueElem* x = popQueue(&_p->removedGrantees);

	if (x == NULL) {
		addToQueue(&_p->lockGrantees, _t);
	}
	else {
		addElemToQueue(&_p->lockGrantees, x, _t);
	}
}

void ungrant(struct Position* _p, struct Node* _t) {
	struct QueueElem* x = removeFromQueue(&_p->lockGrantees, _t, false);

	//add to removedGrantees
	addElemToQueue(&_p->removedGrantees, x, _t);
}

int contains(struct Template* _t, struct Position* _p) {
	int i = 0;
	while (i < _t->size) {
		if (_t->positions[i] == _p)
			return true;
		i = i + 1;
	}
	return false;
}

int Request(int _first, struct Node* _t, struct Node* _l, struct Position* _p) {
	if (_first==true) {
		if (_l->owner == _t) {
			return -1;
		}
		requestEvent(_t, _l, _p);
		checkForCycles(_t);
	}

	if (!_p->inHist) {
		return -1;
	}

	grant(_p, _t);

	int i = 0;
	while (i < histSize) {
		if (contains(&history[i], _p)) {
			if (instance(&history[i], 0)) {
				yieldEvent(_t, &history[i]);
				ungrant(_p, _t);
				checkForCycles(_t);
				return i;
			}
		}
		i = i + 1;
	}

	grantEvent(_t, _l, _p);
	return -1;
}

void Acquired(Thread* t, struct Node* tnode, struct Node* lnode) {
	if (lnode->owner != tnode) {
		lnode->owner = tnode;
		lnode->acqPos = t->reqPos;
		acquiredEvent(tnode, lnode, t->reqPos);
		nSyncs++;
		t->nSyncs++;
		if (t->nSyncs == 1) {
			nSyncThreads++;
		}
	}

	lnode->nLockings = lnode->nLockings + 1;
}

void Release(struct Node* tnode, struct Node* lnode) {
	lnode->nLockings = lnode->nLockings - 1;

	struct Position* p = lnode->acqPos;
	if (lnode->nLockings == 0) {
		lnode->owner = NULL;
		lnode->acqPos = NULL;
		releaseEvent(tnode, lnode);

		if (p->inHist) {
			ungrant(p, tnode);

			int i;
			for (i = 0; i < histSize; i++) {
				if (contains(&history[i], p)) {
					pthread_cond_broadcast(&history[i].avoidanceCondVar);
				}
			}
		}
	}
}

int Template_equals(struct Template* _x, struct Template* _y)	{
	if (_x->size != _y->size)
		return false;

	int i = 0;
	while (i < _x->size) {
		if (Template_equalsFrom(_x, _y, i)) {
			return true;
		}
		i = i + 1;
	}
	return false;
}

int Template_equalsFrom(struct Template* _x, struct Template* _y, int _startIndex)	{
	int i = 0;
	int j = _startIndex;
	while (i < _x->size) {
		if (_x->positions[i] != _y->positions[j]) {
			return false;
		}
		j = Template_next(_startIndex, j, _x->size);
		i++;
	}
	return true;
}

int Template_next(int _start, int _i, int _size) {
	if (_start + _i >= _size)
		return _start + _i - _size;
	else
		return _start + _i;
}

int addToHistory(struct Template* _tmpl) {
	int i = 0;
	while (i < histSize) {
		if (Template_equals(&history[i], _tmpl)) {
			return false;
        }
		i = i + 1;
	}
	history[histSize] = *_tmpl;
	histSize = histSize + 1;
	return true;
}



void initQueue(struct Queue* q){
	q->size = 0;
	q->head = NULL;
	q->tail = NULL;
}

void addToQueue(struct Queue* q, void* data) {
    struct QueueElem *new = malloc_zero(sizeof(struct QueueElem));
    addElemToQueue(q, new, data);
}

void addElemToQueue(struct Queue* q, struct QueueElem* new, void* data) {
	new->elem = data;
	new->next = NULL;

    if(q->size == 0) {
		q->head = new;
		q->tail = new;
	} else {
		q->tail->next = new;
		q->tail = new;
	}

	q->size = q->size + 1;
}

void clearQueue(struct Queue* q) {
	struct QueueElem* x = q->head;

	while (x != NULL) {
		struct QueueElem* next = x->next;
		free(x);
		x = next;
	}

	initQueue(q);
}

int hashOfStack(char* _stack){
    int hash = 0;
    int i;
    for(i=0; _stack[i]!=0; ++i){
        hash += _stack[i];
    }
    return hash % MAXPOSITIONS;
}



void* findInQueue(struct Queue* q, void* _elem){
	if(q->size != 0){
		struct QueueElem * curr = q->head;
		while(curr != NULL){
			if(curr->elem == _elem){
                return curr->elem;
			}else{
				curr = curr->next;
			}
		}
	}
    return NULL;
}

struct Position* findInQueuePos(struct Queue* q, char* _id){

	if(q->size != 0){
		struct QueueElem * curr = q->head;
		while(curr != NULL){
            struct Position* p = (struct Position*)curr->elem;
    	    if(Pos_equals(p, _id)){
                return p;
			}else{
				curr = curr->next;
			}
		}
	}
    return NULL;
}

struct QueueElem* popQueue(struct Queue* q) {
	if (q->size == 0) {
		return NULL;
	}

	struct QueueElem* x = q->head;
	q->head = q->head->next;
	q->size--;
	return x;
}

struct QueueElem* removeFromQueue(struct Queue* q, void* _elem, int doFree) {
	if (q->size == 0) {
		return NULL;
	}

	struct QueueElem* curr = q->head;
	struct QueueElem* prev = NULL;

	while(curr != NULL) {
		if(curr->elem == _elem){
			if (prev == NULL) {
				q->head = curr->next;
			}
			else {
				prev->next = curr->next;
				if (prev->next == NULL) {
					q->tail = prev;
				}
			}
			q->size = q->size-1;
			if (doFree) {
				free(curr);
			}
			return doFree? NULL: curr;
		}
		else {
			prev = curr;
			curr = curr->next;
		}
	}

	return NULL;
}

void before(Thread* t, struct Node* tnode, struct Node* lnode) {
	if (!enabled) {
		t->reqPos = NULL;
		return;
	}

	int size = dvmGetCallStack(t, MAXFRAMES);
	if (filterPos(t->stackBuffer)) {
		t->reqPos = NULL;
		return;
	}

	pthread_mutex_lock(&avoidanceLock);
    t->reqPos = getPosition(t->stackBuffer);

	int first = true;
	while (true) {
		int matchId = -1;
		if (!first) {
			pthread_mutex_lock(&avoidanceLock);
		}
		matchId = Request(first, tnode, lnode, t->reqPos);
		if (matchId >= 0) {
			if (first) {
				nYields++;
//				LOGD("yielding at %s", t->reqPos->id);
			}
			pthread_cond_wait(&history[matchId].avoidanceCondVar, &avoidanceLock);
		}
		pthread_mutex_unlock(&avoidanceLock);

		if (matchId == -1) {
			if (!first) {
//				LOGD("stopped yielding at %s", t->reqPos->id);
			}
			break;
		}
		first = false;
	}
}

void after(Thread* t, struct Node* tnode, struct Node* lnode) {
	if (!enabled || t->reqPos == NULL) {
		lnode->acqPos = NULL;
		return;
	}

	pthread_mutex_lock(&avoidanceLock);
	Acquired(t, tnode, lnode);
	pthread_mutex_unlock(&avoidanceLock);
}

void beforeUnlock(struct Node* tnode, struct Node* lnode) {
	if (!enabled || lnode->acqPos == NULL)
		return;

	pthread_mutex_lock(&avoidanceLock);
	Release(tnode, lnode);
	pthread_mutex_unlock(&avoidanceLock);
}

void* malloc_zero(size_t size) {
	void* mem = malloc(size);

	if (mem != NULL) {
		memset(mem, 0, size);
	}

	return mem;
}








