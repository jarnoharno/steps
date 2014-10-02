package main

// #cgo LDFLAGS: -lm
// #include "madgwick.h"
import "C"
import (
	"log"
	"time"
	"crypto/rand"
	"encoding/hex"
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

	// channel of outbound messages
	send chan *stepsproto.Message

	// current filter
	filter *Filter

	// steal channel
	steal chan chan *Filter
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

type WsMessage struct {
	mt int
	data []byte
}

func (c *connection) ReadLoopWs(out chan WsMessage) {
	defer func() {
		close(out)
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
			return
		}
		out <- WsMessage{
			mt: mt,
			data: data,
		}
	}
}

func (c *connection) DiscardFilter(stopped bool) {
	if c.filter == nil {
		return
	}
	fm.Put(c.filter, stopped)
	c.filter = nil
}

func (c *connection) Steal() chan *Filter {
	ret := make(chan *Filter)
	c.steal <- ret
	return ret
}

func (c *connection) ReadLoop() {
	defer func() {
		c.DiscardFilter(false)
		h.unregister <- c
		c.ws.Close()
	}()
	wsin := make(chan WsMessage)
	go c.ReadLoopWs(wsin)
	for {
		select {
		case s := <-c.steal:
			// give up filter and exit
			s <- c.filter
			c.filter = nil
			return
		case wsmsg, ok := <-wsin:
			if !ok {
				log.Println("close connection")
				return
			}
			mt := wsmsg.mt
			data := wsmsg.data

			if mt == websocket.TextMessage {
				switch string(data) {
				case "listen":
					log.Println("listen")
					h.listen <- c
				}
				continue
			}

			// unmarshal sample
			msg := &stepsproto.Message{}
			err := proto.Unmarshal(data, msg)
			if err != nil {
				log.Println("can't parse data:", err)
				continue
			}

			// check if control message
			switch msg.GetType() {
			case stepsproto.Message_START:

				// generate name
				id := randomString()

				// get filter
				c.DiscardFilter(true)
				c.filter = <-fm.Get(c, id)

				// send ack
				c.send <- &stepsproto.Message{
					Type: stepsproto.Message_START_ACK.Enum(),
					Timestamp: proto.Int64(time.Now().UnixNano()),
					Id: proto.String(id),
				}
			case stepsproto.Message_STOP:
				// ignore trace id
				c.DiscardFilter(true)
			case stepsproto.Message_RESUME:
				id := msg.GetId()
				log.Println("resume trace", id)

				// ignore if we already have the correct trace
				if c.filter != nil && c.filter.name == id {
					continue
				}

				// get filter
				c.DiscardFilter(true)
				c.filter = <-fm.Get(c, id)
			default:
				if c.filter != nil {
					c.filter.Send(msg)
				}
				// else trace not started, ignoring samples
			}
		}
	}
}

func CreateConnection(ws *websocket.Conn) {
	c := &connection{
		ws: ws,
		send: make(chan *stepsproto.Message, 256),
	}
	h.register <- c
	go c.WriteLoop()
	c.ReadLoop()
}

func randomString() string {
	b := make([]byte, 8)
	rand.Read(b)
	return hex.EncodeToString(b)
}
