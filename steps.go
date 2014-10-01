package main

import (
	"flag"
	"log"
	"regexp"
	"net/http"
	"net/url"
	"github.com/gorilla/websocket"
)

var addr = flag.String("addr", "localhost:8080", "service address")
var originRegex = regexp.MustCompile("^((([a-z]+\\.)*whoop\\.pw)|localhost)$")

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		origin := r.Header["Origin"]
		if len(origin) == 0 {
			return true
		}
		u, err := url.Parse(origin[0])
		if err != nil {
			return false
		}
		return originRegex.MatchString(u.Host)
	},
}

func HandleWebSocket(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", 405)
		return
	}
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
		return
	}
	log.Println("new connection")
	CreateConnection(ws)
}

func main() {
	flag.Parse()
	go h.run()
	http.HandleFunc("/", HandleWebSocket)
	err := http.ListenAndServe(*addr, nil)
	if err != nil {
		log.Fatal("ListenAndServe:", err)
	}
}
