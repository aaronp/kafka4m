FROM openjdk:8u191-jre-alpine
#FROM java

# let's get bash
RUN apk update
RUN apk upgrade
RUN apk add bash

# see  https://github.com/docker-library/openjdk/issues/289
RUN apk add --no-cache nss

MAINTAINER "Aaron Pritzlaff"
EXPOSE 80
EXPOSE 9010
RUN mkdir -p /app/web
RUN mkdir -p /app/data
RUN mkdir -p /app/logs
RUN mkdir -p /app/scripts
RUN mkdir -p /app/lib
RUN mkdir -p /app/heapdump/
ENV DATA_DIR /app/data/
ENV JMX_PORT 9010
VOLUME /app/data /app/config /app/logs /app/heapdump /app/web

ADD logback.xml /app/config/logback.xml
ADD application.conf /app/config/application.conf

ADD web /app/web

ADD app.jar /app/lib/app.jar

ADD boot.sh /app/boot.sh
RUN chmod 700 /app/boot.sh
WORKDIR /app

ENTRYPOINT ["/app/boot.sh"]
