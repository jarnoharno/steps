package main

import (
	"flag"
	"log"
	"path"
	"os"
	"os/user"
	"net/http"
	"github.com/gorilla/websocket"
)

var tracedir string

func init() {
	user, err := user.Current()
	if err != nil {
		log.Fatalln("Can't detect current user")
	}
	if user.HomeDir == "" {
		log.Fatalln("Can't detect user home directory")
	}
	tracedir = path.Join(user.HomeDir, ".steps/traces")
	err = os.MkdirAll(tracedir, 0777)
	if err != nil {
		log.Fatalf("Can't create directory for traces (%s)\n", tracedir)
	}
}

var addr = flag.String("addr", "localhost:8080", "service address")

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		// connection is proxied
		return true
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
	go fm.Run()
	http.HandleFunc("/", HandleWebSocket)
	err := http.ListenAndServe(*addr, nil)
	if err != nil {
		log.Fatal("ListenAndServe:", err)
	}
}
