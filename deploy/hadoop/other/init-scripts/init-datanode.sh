#!/bin/bash
mkdir -p /opt/hadoop/data/dataNode

chown -R hadoop:hadoop /opt/hadoop/data/dataNode
chmod 755 /opt/hadoop/data/dataNode

source /hadoop-conf/init-scripts/common-export.sh

hdfs datanode