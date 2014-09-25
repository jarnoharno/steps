#!/bin/bash
pkill steps
cd /home/jao/steps
make
nohup ./steps > steps.out 2> steps.err < /dev/null &
