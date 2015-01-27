package main

import (
	"log"
	"time"
	"./stepsproto"
	"github.com/gorilla/websocket"
	"github.com/gogo/protobuf/proto"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
)

type Connection struct {
	ws *websocket.Conn
	send chan *stepsproto.Message
}

func NewConnection(s *Server, ws *websocket.Conn) *Connection {
	return &Connection{
		ws: ws,
		send: make(chan *stepsproto.Message),
	}
}

func (c *Connection) Run() {
	go c.writeLoop()
	c.readLoop()
}

func (c *Connection) write(mt int, payload []byte) error {
	return c.writedl(mt, payload, time.Now())
}

func (c *Connection) writedl(mt int, payload []byte, now time.Time) error {
	c.ws.SetWriteDeadline(now.Add(writeWait))
	return c.ws.WriteMessage(mt, payload)
}

func (c *Connection) writeLoop() {
	pingTicker := time.NewTicker(pingPeriod)
	defer func() {
		pingTicker.Stop()
	}()
	for {
		select {
		case now := <-pingTicker.C:
			log.Println("PING")
			err := c.writedl(websocket.PingMessage, []byte{}, now);
			if err != nil {
				log.Println(err)
				return
			}
		case msg, ok := <-c.send:
			if !ok {
				c.write(websocket.CloseMessage, []byte{})
				return
			}
			data, err := proto.Marshal(msg)
			if err != nil {
				log.Println(err)
				continue
			}
			err = c.write(websocket.BinaryMessage, data)
			if err != nil {
				log.Println(err)
			}
		}
	}
}

func (c *Connection) readLoop() {
	defer func() {
		// close write loop
		close(c.send)
		// close websocket
		c.ws.Close()
	}()
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
		// ignore text messages
		if mt == websocket.TextMessage {
			continue
		}
		msg := &stepsproto.Message{}
		err = proto.Unmarshal(data, msg)
		// discard invalid messages
		if err != nil {
			log.Println(err)
			continue
		}
		c.handleMessage(msg)
	}
}

func (c *Connection) handleMessage(msg *stepsproto.Message) {
	// TODO protocol
	log.Println(msg)
}
