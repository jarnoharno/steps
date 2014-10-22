package main

import (
	"time"
)

// filter manager

// FilterManager manages the lifecycle of the filters. Filters will be
// discarded after certain time of inactivity.  If stopped is true, filter will
// be discarded immediately. Otherwise the filter will be pooled until timeout.

// manager type

type FilterManager struct {
	get chan *getMsg
	put chan *putMsg
}

var fm = FilterManager{
	get: make(chan *getMsg),
	put: make(chan *putMsg),
}

// get

type getMsg struct {
	conn *connection
	name string
	res chan *Filter
}

func (fm *FilterManager) Get(conn *connection, name string) chan *Filter {
	f := make(chan *Filter)
	fm.get <- &getMsg{conn, name, f}
	return f
}

// put

type putMsg struct {
	filter *Filter
	stopped bool
}

func (fm *FilterManager) Put(filter *Filter, stopped bool) {
	fm.put <- &putMsg{filter, stopped}
}

// implementation

type filterWait struct {
	filter *Filter
	token *int
	cancel *time.Timer
}

func (fw *filterWait) waiting() bool {
	return fw.token != nil
}

func (fw *filterWait) cancelWait() {
	fw.token = nil
	fw.cancel.Stop()
	fw.cancel = nil
}

const filterTimeout = 5 * time.Minute

type expired struct {
	token *int
	name string
}

func removeFilter(filters map[string]*filterWait, name string) {
	filters[name].filter.Stop()
	delete(filters, name)
}

func (fm *FilterManager) Run() {
	filters := make(map[string]*filterWait)
	expire := make(chan expired)
	for {
		select {
		case g := <-fm.get:
			fw := filters[g.name]
			if fw == nil {
				fw = &filterWait{
					filter: NewFilter(g.name),
				}
				filters[g.name] = fw
				g.res <- fw.filter
			} else if fw.waiting() {
				fw.cancelWait()
				g.res <- fw.filter
			} else {
				// connection has been left hanging
				// steal its trace
				g.res <- <-g.conn.Steal()
			}
		case p := <-fm.put:
			if p.stopped {
				removeFilter(filters, p.filter.name)
			} else {
				name := p.filter.name
				token := new(int)
				fw := filters[p.filter.name]
				fw.token = token
				fw.cancel = time.AfterFunc(filterTimeout, func() {
					expire <- expired{token, name}
				})
			}
		case exp := <-expire:
			fw := filters[exp.name]
			if fw != nil && fw.token == exp.token {
				removeFilter(filters, exp.name)
			}
		}
	}
}
