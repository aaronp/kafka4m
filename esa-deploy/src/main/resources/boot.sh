alias myip="ipconfig getifaddr en0"

export IPADDR=`myip`

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
#export JVM_ARGS="$JVM_ARGS -Djava.rmi.server.hostname=$IPADDR"
#export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.port=$JMX_PORT"
export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.authenticate=false"
export JVM_ARGS="$JVM_ARGS -Dcom.sun.management.jmxremote.ssl=false"

echo "Starting w/ $JVM_ARGS on $IPADDR with $@"

java ${JVM_ARGS} -cp /app/lib/app.jar:/app/config esa.rest.Main $@