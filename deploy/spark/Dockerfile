FROM bitnamilegacy/spark:4.0.0-debian-12-r17

USER root

RUN echo '*.sink.prometheusServlet.class=org.apache.spark.metrics.sink.PrometheusServlet' > ${SPARK_HOME}/conf/metrics.properties && \
    echo '*.sink.prometheusServlet.path=/metrics/prometheus' >> ${SPARK_HOME}/conf/metrics.properties