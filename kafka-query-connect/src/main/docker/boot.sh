#!/usr/bin/env bash

nohup /usr/sbin/mosquitto -c /mosquitto/config/mosquitto.conf &

export JMX_PORT=${JMX_PORT:-9990}

echo "Starting w/ $HOST_IP and $JMX_PORT"

ls /usr/sbin/mosquitto
export JVM_ARGS="-server"
export JVM_ARGS="$JVM_ARGS -Xms1g -Xmx1g"
export JVM_ARGS="$JVM_ARGS -XX:MaxMetaspaceSize=256m"
export JVM_ARGS="$JVM_ARGS -Xmn100m"
export JVM_ARGS="$JVM_ARGS -XX:SurvivorRatio=6"
export JVM_ARGS="$JVM_ARGS -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled"
export JVM_ARGS="$JVM_ARGS -XX:+PrintGCDateStamps -verbose:gc -XX:+PrintGCDetails -Xloggc:/app/logs/gc.log"
export JVM_ARGS="$JVM_ARGS -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
export JVM_ARGS="$JVM_ARGS -Dsun.net.inetaddr.ttl=3600"
export JVM_ARGS="$JVM_ARGS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/heapdump/dump.hprof"

export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.local.only=false"
export JVM_ARGS="$JVM_ARGS -Djava.rmi.server.hostname=${HOST_IP}"
export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.port=$JMX_PORT"
export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.authenticate=false"
export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.ssl=false"

export USER_ARGS="$@"
echo "Starting w/ $JVM_ARGS w/ HOST_IP '$HOST_IP' with $# user args: ${USER_ARGS}"

java ${JVM_ARGS} -cp /app/config:/app/lib/app.jar kafkaquery.Main ${USER_ARGS}

echo "done"