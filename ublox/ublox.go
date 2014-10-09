package main

import (
    "log"
	"time"
    "github.com/gorilla/websocket"
)

func RunWebsocket(start chan string, stop chan string) {
    c, _, err := websocket.DefaultDialer.Dial("wss://whoop.pw/steps/ws", nil)
    defer func() {
        c.Close()
        time.Sleep(5 * time.Second)
    }()
    if err != nil {
        log.Println(err)
        return
    }
    for {
        mt, _, err := c.ReadMessage()
        if err != nil {
            log.Println(err)
            return
        }
        if mt != websocket.BinaryMessage {
            continue
        }
    }
}

func main() {
    start := make(chan string)
    stop := make(chan string)

    RunGps(start, stop)
    //for {
    //    RunWebsocket(start, stop)
    //}
}
