package main

import (
    "log"
    "time"
    "regexp"
    "net/url"
    "net/http"
    "github.com/gorilla/websocket"
	"code.google.com/p/goprotobuf/proto"
	"./stepsproto"
)

const (
    port = "8080"
    writeWait = 10 * time.Second
    pongWait = 60 * time.Second
    pingPeriod = (pongWait * 9) / 10
    maxMessageSize = 512
)

//var originRegex = regexp.MustCompile("^([a-z]+\\.)*whoop\\.pw$")
var originRegex = regexp.MustCompile("^localhost$")

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

type Packet struct {
    Msg string
}

func Write(ws *websocket.Conn) {
    ticker := time.NewTicker(pingPeriod)
    defer func() {
        ticker.Stop()
        ws.Close()
    }()
    for {
        instant := <-ticker.C
		log.Println("PING")
        ws.SetWriteDeadline(instant.Add(writeWait))
        err := ws.WriteMessage(websocket.PingMessage, []byte{})
        if err != nil {
            log.Println(err)
            break
        }
    }
}

func Handle(w http.ResponseWriter, r *http.Request) {
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
    defer func() {
        ws.Close()
    }()
    go Write(ws)
    ws.SetReadLimit(maxMessageSize)
    ws.SetReadDeadline(time.Now().Add(pongWait))
    ws.SetPongHandler(func(string) error {
		log.Println("PONG")
        ws.SetReadDeadline(time.Now().Add(pongWait))
        return nil
    })
    var packet Packet
    for {
        err := ws.ReadJSON(&packet)
        if err != nil {
            log.Println(err)
            break
        }
        log.Println("msg:", packet.Msg)
    }
}

func main() {
	sample := &stepsproto.Sample {
		Type: proto.String("acc"),
		Timestamp: proto.Uint64(34),
		Value: []float32{0.0, 0.0, 9.8},
	}
	data, err := proto.Marshal(sample)
	if err != nil {
		log.Fatal("marshaling error:", err)
	}

	newSample := &stepsproto.Sample{}
	err = proto.Unmarshal(data, newSample)
	if err != nil {
		log.Fatal("unmarshaling error:", err)
	}
	log.Println("data:", len(data), "bytes", data)
	log.Println("sample:", sample)
	log.Println("newSample:", newSample)

	http.HandleFunc("/", Handle)
    err = http.ListenAndServe("localhost:" + port, nil)
	if err != nil {
		panic("ListenAndServe: " + err.Error())
	}
}
