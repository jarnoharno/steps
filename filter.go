package main

import (
	"log"
	"time"
	"./stepsproto"
	"code.google.com/p/goprotobuf/proto"
)

type Trace struct {
	send chan *stepsproto.Sample
	done chan struct{}
}

var traces = make(map[string]*Trace)

const (
	traceTimeout = 5 * time.Minute
)

func (Trace *t) run() {
	ticker := time.NewTicker(traceTimeout)
	defer func() {
		ticker.Stop()
		close(t.done)
	}
	for {
		select {
		case <-ticker.C:
			// trace expired
			return
		case sample, ok := <-t.send:
			if !ok {
				return
			}
			ticker.Stop()
			filter(sample)
			ticker = time.NewTicker(traceTimeout)
		}
	}
}

func filter(sample *stepsproto.Sample) {
	h.broadcast <- sample
}

// filters

// 10 ms
const resampleTime = 10000000

// sample types to be multiplexed
var types = [...]string{"acc", "gyr", "mag"}

type SampleMap map[string]*stepsproto.Sample

func (s SampleMap) defined() bool {
	return len(s) == len(types)
}

// resampled and multiplexed collection of samples
type MuxSample struct {
	timestamp int64
	samples SampleMap
	next *MuxSample
}

// first element in mux queue
var first = &MuxSample{}

// resample range
type Range struct {
	timestamp int64
	prev *stepsproto.Sample
	next *stepsproto.Sample
}

// value ranges
var ranges = map[string]*Range{}

func init() {
	for i := range types {
		ranges[types[i]] = &Range{}
	}
}

func prevDefined() bool {
	for _, r := range ranges {
		if r.prev == nil {
			return false
		}
	}
	return true
}

var started = false
var lastTimestamp = int64(0)

func mux(sample *stepsproto.Sample) {
	if sample.GetType() != stepsproto.Sample_SENSOR_EVENT {
		log.Println(sample.GetType())
		return
	}
	log.Println(sample.GetType(), sample.GetName(), sample.GetTimestamp())
	if !started {
		ranges[sample.GetName()].prev = sample
		if prevDefined() {
			log.Println("got them all!")
			started = true
			return
		}
	}

	if sample.GetName() == "acc" {
		if lastTimestamp != 0 {
		log.Println(sample.GetType(), sample.GetName(),
			(sample.GetTimestamp() - lastTimestamp) / 1000)
		}
		lastTimestamp = sample.GetTimestamp()
	}

	if false {
		newSample := &stepsproto.Sample{
			Name: proto.String("asdf"),
			Timestamp: proto.Int64(123),
			Type: stepsproto.Sample_SENSOR_EVENT.Enum(),
		}
		h.broadcast <- newSample
	}
}
