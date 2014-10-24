package main

import (
	"fmt"
	"os"
	"bufio"
	"io"
	"time"
	"../server/stepsproto"
	protoio "code.google.com/p/gogoprotobuf/io"
)

func eprint(err error) {
	fmt.Fprintln(os.Stderr, "Error:", err)
}

func eclose(c io.Closer) {
	err := c.Close()
	if err != nil {
		eprint(err)
	}
}

func usage() {
	fmt.Fprintf(os.Stderr, "Usage: %s input\n", os.Args[0])
}

func main() {
	if len(os.Args) != 2 {
		usage()
		os.Exit(1)
	}
	fn := os.Args[1]
	if err := run(fn); err != nil {
		eprint(err)
		os.Exit(1)
	}
}

func run(filename string) error {
	f, err := os.Open(filename)
	if err != nil {
		return err
	}
	defer eclose(f)
	br := bufio.NewReader(f)
	rc := protoio.NewDelimitedReader(br, 1024)
	defer eclose(rc)
	return read(rc)
}

func read(r protoio.Reader) error {
	msg := stepsproto.Message{}
	for {
		err := r.ReadMsg(&msg)
		if err != nil {
			// eof is expected
			if err == io.EOF {
				break
			}
			return err
		}
		msgprint(&msg)
	}
	return nil
}

func msgprint(msg *stepsproto.Message) {
	switch *msg.Type {
	case stepsproto.Message_SENSOR_EVENT:
		fmt.Printf("%s %s", ts(*msg.Timestamp), *msg.SensorId)
		for _, v := range msg.Value {
			fmt.Printf(" %.8f", v)
		}
		fmt.Println()
	case stepsproto.Message_LOCATION:
		fmt.Printf("%s %s %s %.8f %.8f %.2f %.2f %.2f %.2f\n",
			ts(*msg.Timestamp),
			*msg.SensorId,
			ts(*msg.Utctime * 1000000),
			*msg.Latitude,
			*msg.Longitude,
			*msg.Accuracy,
			*msg.Altitude,
			*msg.Bearing,
			*msg.Speed)
	}
}

func ts(nsec int64) string {
	//time.Unix(0, *msg.Timestamp).Format(time.RFC3339),
	return time.Unix(0, nsec).Format("2006/01/02 15:04:05.000")
}
