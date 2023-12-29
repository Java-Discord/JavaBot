FROM eclipse-temurin:21-jre
RUN mkdir /work
COPY build/libs/JavaBot-1.0.0-SNAPSHOT-all.jar /work/bot.jar
WORKDIR /work
RUN chown 1000:1000 /work
VOLUME "/work/config"
VOLUME "/work/logs"
VOLUME "/work/db"
VOLUME "/work/purgeArchives"
USER 1000
ENTRYPOINT [ "java", "-jar", "bot.jar" ]