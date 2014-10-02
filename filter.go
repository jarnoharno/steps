package main

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

func (f *Filter) complete() bool {
	for _, r := range f.ranges {
		if r.last == nil {
			return false
		}
	}
	return true
}

func (f *Filter) outputInterpolate(
		id string,
		timestamp int64,
		values []float32) {
	log.Println(id, timestamp, values, "interp")
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
			// initialize first muxed message
			f.first = &MuxMessage{
				timestamp: timestamp,
				values: ValueMap{},
			}
			// add first values
			f.first.values[id] = vals
			// set range timestamps
			for rid, r := range f.ranges {
				if rid == id {
					r.timestamp = timestamp + resamplePeriod
				} else {
					r.timestamp = timestamp
				}
			}
			log.Println(id, timestamp, vals, "first")
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
