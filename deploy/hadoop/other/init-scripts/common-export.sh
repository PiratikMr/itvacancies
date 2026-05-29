#!/bin/bash

export HADOOP_OPTS="$HADOOP_OPTS -javaagent:/hadoop-conf/jmx-exporter/jmx_prometheus_javaagent.jar=$JMX_PORT:/hadoop-conf/jmx-exporter/$JMX_FILE_NAME.yml"
export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=$JMX_PORT Dcom.sun.management.jmxremote.rmi.port=$JMX_PORT -Djava.rmi.server.hostname=$HOSTNAME"
