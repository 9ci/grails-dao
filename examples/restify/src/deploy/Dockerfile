FROM openjdk:8-jdk-alpine
# see the ./scripts/docker.sh build that copies it to build/docker and add the neccesary files first

#Jasper reports needs this
RUN apk add ttf-dejavu fontconfig

# keeps the volumes from run to run
VOLUME /tmp
VOLUME /app/conf
VOLUME /app/resources
VOLUME /app/logs

WORKDIR /app


# COPY build/libs/*.jar app.jar
# ENTRYPOINT ["java","-Dloader.path=/extData/conf/", "-jar","/app.jar"]

#ARG LIB_DIR=build/libs
ARG LIB_DIR=.
ARG APP_JAR_DIR=/app/jar

COPY ${LIB_DIR}/BOOT-INF/lib ${APP_JAR_DIR}/BOOT-INF/lib
COPY ${LIB_DIR}/org ${APP_JAR_DIR}/org
#/META-INF has version in it so it wont get changes unless version bumps
COPY ${LIB_DIR}/META-INF ${APP_JAR_DIR}/META-INF
#the default resources for demo
#COPY ${LIB_DIR}/rootLocation /app/resources
# the app classes where the bulk of the changes will be, so do this last so the above can be layer cached
COPY ${LIB_DIR}/BOOT-INF/classes ${APP_JAR_DIR}/BOOT-INF/classes

COPY docker-entrypoint.sh /usr/local/bin/

ENTRYPOINT ["docker-entrypoint.sh"]

# Reference
# http://www.projectatomic.io/docs/docker-image-author-guidance/
# https://github.com/EtienneK/spring-boot-docker-base
