package main

import (
	"log"
	"net/http"
	"github.com/gorilla/websocket"
)

type DeviceMap map[DeviceId]*Device
type ConnectionMap map[*Connection]struct{}

type Server struct {
	Devices DeviceMap
	Connections ConnectionMap

	upgrader websocket.Upgrader
}

func NewServer() *Server {
	return &Server{
		Devices: make(DeviceMap),
		Connections: make(ConnectionMap),
		upgrader: websocket.Upgrader{
			CheckOrigin: func(r *http.Request) bool {
				// we're listening to localhost only so connection is proxied
				// and the proxy handles origin checks
				return true
			},
		},
	}
}

func (s *Server) Run() error {
	mux := http.NewServeMux()
	mux.HandleFunc("/", s.handle)
	srv := http.Server{
		Addr: "127.0.0.1:8080",
		Handler: mux,
	}
	return srv.ListenAndServe()
}

func (s *Server) handle(res http.ResponseWriter, req *http.Request) {
	ws, err := s.upgrader.Upgrade(res, req, nil)
	if err != nil {
		log.Println(err)
		return
	}
	log.Println("new connection:", req.Method, req.URL, "from", req.RemoteAddr)
	c := NewConnection(s, ws)
	s.Connections[c] = struct{}{}
	c.Run()
}
