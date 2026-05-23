FROM apache/airflow:2.10.2
USER root

RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
    hdfs-cli \
    openjdk-17-jdk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV HADOOP_NAMENODE namenode:9000
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64/


USER airflow

RUN echo "SESSION_COOKIE_NAME = 'airflow-session'" >> $AIRFLOW_HOME/webserver_config.py

COPY ./requirements.txt /
RUN pip install --no-cache-dir -r /requirements.txt