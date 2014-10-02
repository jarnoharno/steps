package main

import (
	"./stepsproto"
)

type hub struct {

	// all connections
	connections map[*connection]bool

	// listeners
	listeners map[*connection]bool

	// inbound samples
	broadcast chan *stepsproto.Sample

	// register requests
	register chan *connection

	// unregister requests
	unregister chan *connection

	// listen requests
	listen chan *connection
}

var h = hub{
	connections: make(map[*connection]bool),
	listeners:   make(map[*connection]bool),
	broadcast:   make(chan *stepsproto.Sample, 256),
	register:    make(chan *connection),
	unregister:  make(chan *connection),
	listen:      make(chan *connection),
}

func (h *hub) run() {
	for {
		select {
		case c := <-h.register:
			h.connections[c] = true
		case c := <-h.unregister:
			h.unreg(c)
		case c:= <-h.listen:
			h.listeners[c] = true
		case m := <-h.broadcast:
			for c := range h.listeners {
				select {
				case c.send <- m:
				default:
					h.unreg(c)
				}
			}
		}
	}
}

func (h* hub) unreg(c *connection) {
	if _, ok := h.listeners[c]; ok {
		delete(h.listeners, c)
	}
	if _, ok := h.connections[c]; ok {
		delete(h.connections, c)
		close(c.send)
	}
}
