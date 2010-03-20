#!/bin/bash
#echo off
export MYCLASSPATH=./properties
export MYCLASSPATH=${MYCLASSPATH}:./bin
for i in `ls ./lib/*.jar`
do 
	export MYCLASSPATH=${MYCLASSPATH}:${i}
done
echo ${MYCLASSPATH}
java -classpath ${MYCLASSPATH} -Xms256m -Xmx384m com.anji.neat.Evolver $1 &
