package main

import (
	"log"
	"./stepsproto"
	"code.google.com/p/goprotobuf/proto"
)

// 10 ms
const remsgTime = 25000000

// msg types to be multiplexed
var types = [...]string{"acc", "gyr", "mag"}

type MessageMap map[string]*stepsproto.Message

func (s MessageMap) defined() bool {
	return len(s) == len(types)
}

// remsgd and multiplexed collection of msgs
type MuxMessage struct {
	timestamp int64
	msgs MessageMap
	next *MuxMessage
}

// remsg range
type Range struct {
	timestamp int64
	prev *stepsproto.Message
	next *stepsproto.Message
}

type Filter struct {

	name string

	// first element in mux queue
	first *MuxMessage

	// interpolation ranges
	ranges map[string]*Range

	// first interpolation instant defined
	started bool
}

func NewFilter(name string) *Filter {
	log.Println("start trace", name)
	filter := &Filter{
		name: name,
		ranges: make(map[string]*Range),
	}
	for i := range types {
		filter.ranges[types[i]] = &Range{}
	}
	return filter
}

func (f *Filter) prevDefined() bool {
	for _, r := range f.ranges {
		if r.prev == nil {
			return false
		}
	}
	return true
}

func (f *Filter) Send(msg *stepsproto.Message) {
	h.broadcast <- msg
}

// filters

func (f *Filter) mux(msg *stepsproto.Message) {
	if msg.GetType() != stepsproto.Message_SENSOR_EVENT {
		log.Println(msg.GetType())
		return
	}
	log.Println(msg.GetType(), msg.GetId(), msg.GetTimestamp())
	if !f.started {
		f.ranges[msg.GetId()].prev = msg
		if f.prevDefined() {
			log.Println("got them all!")
			f.started = true
			return
		}
	}

	if false {
		newMsg := &stepsproto.Message{
			Type: stepsproto.Message_SENSOR_EVENT.Enum(),
			Id: proto.String("asdf"),
			Timestamp: proto.Int64(123),
		}
		h.broadcast <- newMsg
	}
}
