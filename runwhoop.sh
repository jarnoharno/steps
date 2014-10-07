#!/bin/bash
export STEPSPATH=$HOME/.steps
export GOPATH=$HOME/go
pkill steps
mkdir -p $STEPSPATH
cd $HOME/steps
make
nohup ./server/steps > $STEPSPATH/steps.out 2>&1 < /dev/null &
