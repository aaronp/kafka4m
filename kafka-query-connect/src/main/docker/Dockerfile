FROM java

# TODO - use alpine java

COPY app.jar /app/lib/app.jar
ADD boot.sh /app/boot.sh
RUN chmod +x /app/boot.sh
WORKDIR /app

VOLUME /app/config

EXPOSE 9001/tcp
EXPOSE 1883/tcp
EXPOSE 9990/tcp

ENTRYPOINT /app/boot.sh $@