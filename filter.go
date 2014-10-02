package main

// #cgo LDFLAGS: -lm
// #include "madgwick.h"
import "C"

import (
	"log"
	"./stepsproto"
//	"code.google.com/p/goprotobuf/proto"
)

// 25 ms
const resamplePeriod = 25000000

// msg types to be multiplexed
var types = [...]string{"acc", "gyr", "mag"}

type ValueMap map[string][]float32

func (s ValueMap) complete() bool {
	return len(s) == len(types)
}

// remsgd and multiplexed collection of msgs
type MuxMessage struct {
	timestamp int64
	values ValueMap
	next *MuxMessage
}

// remsg range
type Range struct {

	// next interpolated timestamp
	timestamp int64
	last *stepsproto.Message
}

type Filter struct {

	name string

	// empty root element in mux queue
	root *MuxMessage

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
		root: &MuxMessage{},
	}
	for i := range types {
		filter.ranges[types[i]] = &Range{}
	}
	return filter
}

func (f *Filter) complete() bool {
	for _, r := range f.ranges {
		if r.last == nil {
			return false
		}
	}
	return true
}

func (f *Filter) outputMux(msg *MuxMessage) {
	id := ""
	values := make([]float32, 0)
	for _, t := range types {
		id += t[0:1]
		values = append(values, msg.values[t]...)
	}
	log.Println(id, msg.timestamp, values, "mux")
}

func (f *Filter) outputInterpolate(
		id string,
		timestamp int64,
		values []float32) {
	log.Println(id, timestamp, values, "interp")

	prev := f.root
	for prev.next != nil && prev.next.timestamp < timestamp {
		prev = prev.next
	}
	if prev.next == nil {
		m := &MuxMessage{
			timestamp: timestamp,
			values: ValueMap{},
		}
		prev.next = m
	}
	prev.next.values[id] = values
	if !prev.next.values.complete() {
		return
	}
	msg := prev.next
	prev.next = msg.next
	f.outputMux(msg)
}

// entry point

func (f *Filter) Send(msg *stepsproto.Message) {
	f.mux(msg)
	h.broadcast <- msg
}

func (f *Filter) mux(msg *stepsproto.Message) {

	if msg.GetType() != stepsproto.Message_SENSOR_EVENT {
		return
	}

	id := msg.GetId()
	vals := msg.GetValue()
	timestamp := msg.GetTimestamp()

	defer log.Println(id, timestamp, vals)

	if !f.started {
		f.ranges[id].last = msg
		if f.complete() {
			// set range timestamps
			for rid, r := range f.ranges {
				if rid == id {
					r.timestamp = timestamp + resamplePeriod
				} else {
					r.timestamp = timestamp
				}
			}
			f.outputInterpolate(id, timestamp, vals)
			f.started = true
		}
		return
	}

	r := f.ranges[id]
	if timestamp < r.timestamp {
		r.last = msg
		return
	}

	// calculate interpolated values

	sampleCount := int(timestamp - r.timestamp) / resamplePeriod + 1
	prevValues := r.last.GetValue()
	prevTimestamp := r.last.GetTimestamp()
	prevTimeDiff := timestamp - prevTimestamp

	deltas := make([]float64, len(prevValues))
	values := make([]float32, len(prevValues))

	// calculate deltas
	for i := range vals {
		valueDiff := float64(vals[i]) - float64(prevValues[i])
		deltas[i] = valueDiff / float64(prevTimeDiff)
	}

	// interpolate
	t := r.timestamp
	for j := 0; j < sampleCount; j++ {
		dt := t - prevTimestamp
		for i := range prevValues {
			values[i] = float32(float64(prevValues[i]) +
				deltas[i] * float64(dt))
		}
		f.outputInterpolate(id, t, values)
		t += resamplePeriod
	}

	// restore last
	r.last = msg
	r.timestamp = t
}
