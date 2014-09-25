#!/bin/bash
export GOPATH=/home/jao/go
pkill steps
cd /home/jao/steps
make
nohup ./steps > steps.out 2>&1 < /dev/null &
