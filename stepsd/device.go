package main

type DeviceId uint32
type DeviceKey uint32

type Device struct {

	// unique device id
	Id DeviceId

	// human readable name
	Name string

	// server generated key
	Key DeviceKey
}
