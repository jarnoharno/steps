package main

// #include <stdlib.h>
// #include <gps.h>
// #cgo LDFLAGS: -lgps
import "C"

import (
	"fmt"
	"bufio"
	"errors"
	"syscall"
	"unsafe"
	"os"
	"net"
	"time"
	"math"
)

const (
	DefaultGpsdPort = C.DEFAULT_GPSD_PORT
	WatchDisable = C.WATCH_DISABLE
	WatchEnable = C.WATCH_ENABLE
	WatchJson = C.WATCH_JSON
	Mode2D = C.MODE_2D
)

type GpsData C.struct_gps_data_t

func GpsOpen(host string, port string, gpsData *GpsData) error {
	g := (*C.struct_gps_data_t) (gpsData)
	h := C.CString(host)
	defer C.free(unsafe.Pointer(h))
	p := C.CString(port)
	defer C.free(unsafe.Pointer(p))
	b, err := C.gps_open(h, p, g)
    if b != 0 {
		errno := err.(syscall.Errno)
		return errors.New(C.GoString(C.gps_errstr(C.int(errno))))
	}
	return nil
}

func GpsStream(gpsData *GpsData, intflags uint) error {
	g := (*C.struct_gps_data_t) (gpsData)
	b, err := C.gps_stream(g, C.uint(intflags), nil)
	if b != 0 {
		errno := err.(syscall.Errno)
		return errors.New(C.GoString(C.gps_errstr(C.int(errno))))
	}
	return nil
}

func GpsUnpack(buf []byte, gpsData *GpsData) error {
	g := (*C.struct_gps_data_t) (gpsData)
	b, err := C.gps_unpack((*C.char) (unsafe.Pointer(&buf[0])), g)
	if b != 0 {
		errno := err.(syscall.Errno)
		return errors.New(C.GoString(C.gps_errstr(C.int(errno))))
	}
	return nil
}

func loop(gpsData *GpsData) {
	g := (*C.struct_gps_data_t) (gpsData)
	f := os.NewFile(uintptr(g.gps_fd), "")
	c, err := net.FileConn(f)
	prevTime := g.fix.time
	if err != nil {
		fmt.Println(err)
		return
	}
	r := bufio.NewReader(c)
	var b []byte
	for {
		b, err = r.ReadBytes('\n')
		if err != nil {
			fmt.Println(err)
			return
		}
		nanoTime := time.Now().UnixNano()
		err = GpsUnpack(b, gpsData)
		if err != nil {
			fmt.Println(err)
		}
		if g.fix.time == prevTime || g.fix.mode < Mode2D {
			continue
		}
		prevTime = g.fix.time
		fixTime := int64(g.fix.time * 1e3)
		accuracy := math.Sqrt(float64(g.fix.epx) * float64(g.fix.epy)) / 2.0
		fmt.Printf("%d %d %.8f %.8f %f %f %f %f\n",
			nanoTime,
			fixTime,
			g.fix.latitude,
			g.fix.longitude,
			accuracy,
			g.fix.altitude,
			g.fix.track,
			g.fix.speed,
		)
	}
}

func main() {
	gpsData := &GpsData{}
	err := GpsOpen("localhost", DefaultGpsdPort, gpsData)
	if err != nil {
		fmt.Println(err)
		return
	}
	err = GpsStream(gpsData, WatchEnable | WatchJson)
	if err != nil {
		fmt.Println(err)
		return
	}
	loop(gpsData)
	err = GpsStream(gpsData, WatchDisable)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println(gpsData.fix.time)
}
