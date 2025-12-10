#!/bin/bash

echo "Starting 3 workers..."
mvn exec:java -Dexec.mainClass="com.example.distributed.DistributedWorker" -Dexec.args="worker1 5001" > /tmp/worker1.log 2>&1 &
PID1=$!
sleep 2

mvn exec:java -Dexec.mainClass="com.example.distributed.DistributedWorker" -Dexec.args="worker2 5002" > /tmp/worker2.log 2>&1 &
PID2=$!
sleep 2

mvn exec:java -Dexec.mainClass="com.example.distributed.DistributedWorker" -Dexec.args="worker3 5003" > /tmp/worker3.log 2>&1 &
PID3=$!
sleep 2

echo "Workers started! PIDs: $PID1, $PID2, $PID3"
echo ""
echo "Running distributed example..."
mvn exec:java -Dexec.mainClass="com.example.ExampleDistributed"

echo ""
echo "Stopping workers..."
kill $PID1 $PID2 $PID3 2>/dev/null
