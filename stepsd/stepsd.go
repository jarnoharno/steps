package main

import (
	"log"
)

func main() {
	s := NewServer()
	err := s.Run()
	if err != nil {
		log.Fatal("Error: ", err)
	}
}
