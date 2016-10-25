#!/bin/bash

# Debug option
DEBUG_PARAMS=""
for arg in "$@"
do
    if [ "$arg" == "--debug" ]; then
      echo "setting java process as debuggable"
      DEBUG_PARAMS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1060"
      shift
    fi
done

java $DEBUG_PARAMS -jar sbt-launch.jar "$@"
