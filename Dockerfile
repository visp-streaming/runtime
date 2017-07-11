FROM jeanblanchard/java:8
MAINTAINER Christoph Hochreiner <ch.hochreiner@gmail.com>
COPY target/runtime-0.1-SNAPSHOT.jar app.jar
COPY runtimeConfiguration runtimeConfiguration
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
