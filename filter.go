package main

import (
	"log"
	"./stepsproto"
	"code.google.com/p/goprotobuf/proto"
)

// 10 ms
const resampleTime = 25000000

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

// resample range
type Range struct {
	timestamp int64
	prev *stepsproto.Sample
	next *stepsproto.Sample
}

type Filter struct {

	name string

	// first element in mux queue
	first *MuxSample

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

func (f *Filter) Send(sample *stepsproto.Sample) {
	h.broadcast <- sample
}

// filters

func (f *Filter) mux(sample *stepsproto.Sample) {
	if sample.GetType() != stepsproto.Sample_SENSOR_EVENT {
		log.Println(sample.GetType())
		return
	}
	log.Println(sample.GetType(), sample.GetName(), sample.GetTimestamp())
	if !f.started {
		f.ranges[sample.GetName()].prev = sample
		if f.prevDefined() {
			log.Println("got them all!")
			f.started = true
			return
		}
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
