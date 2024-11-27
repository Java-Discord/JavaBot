FROM alpine:latest
RUN apk add --no-cache libsm libxrender libxext libxtst libxi gcompat ttf-dejavu

COPY build/native/nativeCompile /work
WORKDIR /work

RUN chown 1000:1000 /work
USER 1000
ENV HOME=/work

# https://github.com/openjdk/jdk/pull/20169
# need to create fake JAVA_HOME
RUN mkdir -p /tmp/JAVA_HOME/conf/fonts
RUN mkdir /tmp/JAVA_HOME/lib


VOLUME "/work/config"
VOLUME "/work/logs"
VOLUME "/work/db"
VOLUME "/work/purgeArchives"
ENTRYPOINT [ "./javabot", "-Djava.home=/tmp/JAVA_HOME" ]
