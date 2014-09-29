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

type Packet struct {
    Msg string
}

func Write(ws *websocket.Conn, c chan *stepsproto.Sample) {
    ticker := time.NewTicker(pingPeriod)
    defer func() {
        ticker.Stop()
        ws.Close()
    }()
    for {
        select {
        case instant := <-ticker.C:
            log.Println("PING")
            ws.SetWriteDeadline(instant.Add(writeWait))
            err := ws.WriteMessage(websocket.PingMessage, []byte{})
            if err != nil {
                log.Println(err)
                return
            }
        case sample := <-c:
            data, err := proto.Marshal(sample)
            if err != nil {
                log.Println(err)
            }
            ws.SetWriteDeadline(time.Now().Add(writeWait))
            err = ws.WriteMessage(websocket.BinaryMessage, data)
            if err != nil {
                log.Println(err)
                return
            }
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
    c := make(chan *stepsproto.Sample)
    go Write(ws, c)
    ws.SetReadLimit(maxMessageSize)
    ws.SetReadDeadline(time.Now().Add(pongWait))
    ws.SetPongHandler(func(string) error {
		log.Println("PONG")
        ws.SetReadDeadline(time.Now().Add(pongWait))
        return nil
    })
    for {
        mt, data, err := ws.ReadMessage()
        if err != nil {
            log.Println(err)
            return
        }
        if mt == websocket.TextMessage {
            log.Println("text:", string(data))
            continue
        }
        log.Println("bin:", data)
        sample := &stepsproto.Sample{}
        err = proto.Unmarshal(data, sample)
        if err != nil {
            log.Println("can't parse data:", err)
            continue
        }
        log.Println("sample:", sample)
        c <- sample
    }
}

func main() {
	sample := &stepsproto.Sample {
		Type: proto.String("acc"),
		Timestamp: proto.Int64(time.Now().UnixNano()),
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
