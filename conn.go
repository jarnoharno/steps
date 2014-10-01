package main

// #cgo LDFLAGS: -lm
// #include "madgwick.h"
import "C"
import (
	"log"
	"time"
	"./stepsproto"
	"github.com/gorilla/websocket"
	"code.google.com/p/goprotobuf/proto"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 512
)

type connection struct {

	// websocket connection
	ws *websocket.Conn

	// channel of outbound samples
	send chan *stepsproto.Sample

	// current filter
	filter *Filter
}

func (c *connection) write(mt int, payload []byte) error {
	return c.writedl(mt, payload, time.Now())
}

func (c *connection) writedl(mt int, payload []byte, now time.Time) error {
	c.ws.SetWriteDeadline(now.Add(writeWait))
	return c.ws.WriteMessage(mt, payload)
}

func (c *connection) WriteLoop() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.ws.Close()
	}()
	for {
		select {
		case instant := <-ticker.C:
			log.Println("PING")
			if err := c.writedl(websocket.PingMessage, []byte{}, instant);
				err != nil {
				log.Println(err)
				return
			}
		case sample, ok := <-c.send:
			if !ok {
				c.write(websocket.CloseMessage, []byte{})
				return
			}
			// marshal sample
			data, err := proto.Marshal(sample)
			if err != nil {
				log.Println(err)
				continue
			}
			if err = c.write(websocket.BinaryMessage, data); err != nil {
				log.Println(err)
				return
			}
		}
	}
}

func (c *connection) ReadLoop() {
	defer func() {
		h.unregister <- c
		c.ws.Close()
	}()
	c.ws.SetReadLimit(maxMessageSize)
	c.ws.SetReadDeadline(time.Now().Add(pongWait))
	c.ws.SetPongHandler(func(string) error {
		log.Println("PONG")
		c.ws.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})
	for {
		mt, data, err := c.ws.ReadMessage()
		if err != nil {
			log.Println(err)
			return
		}
		if mt == websocket.TextMessage {
			switch string(data) {
			case "listen":
				log.Println("listen")
				h.listen <- c
			}
			continue
		}
		// unmarshal sample
		sample := &stepsproto.Sample{}
		err = proto.Unmarshal(data, sample)
		if err != nil {
			log.Println("can't parse data:", err)
			continue
		}
		// check if control message
		if sample.GetType() == stepsproto.Sample_CONTROL {
			ctrl := sample.GetControl()
			switch ctrl.GetType() {
			case stepsproto.Control_START:
				log.Println("trace started")
				c.filter = NewFilter()
				c.send <- &stepsproto.Sample{
					Name: proto.String("ctrl"),
					Timestamp: proto.Int64(time.Now().UnixNano()),
					Type: stepsproto.Sample_CONTROL.Enum(),
					Control: &stepsproto.Control{
						Type: stepsproto.Control_START_ACK.Enum(),
						StartAck: &stepsproto.StartAck{
							Id: proto.String("asdf"),
						},
					},
				}
			case stepsproto.Control_STOP:
				log.Println("trace stopped")
				c.filter = nil
			case stepsproto.Control_RESUME:
				log.Println("trace resumed")
			}
		} else if c.filter != nil {
			c.filter.Send(sample)
		}
	}
}

func CreateConnection(ws *websocket.Conn) {
	c := &connection{
		ws: ws,
		send: make(chan *stepsproto.Sample, 256),
	}
	h.register <- c
	go c.WriteLoop()
	c.ReadLoop()
}

