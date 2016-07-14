#!/bin/bash

for i in $(find /data/riak -type f -name *data) ;do 
	java -jar bitcask-0.0.2.jar -k keys -l info $i
	sleep 2;
done
