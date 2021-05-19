#!/bin/sh

echo "$0: Starting restify"
# bash cheat sheet https://devhints.io/bash
# sets default values
: ${EXTRA_JAVA_OPTS:=-Xmx2048m -XX:MaxMetaspaceSize=256m}
: ${JAVA_OPTS:=-server -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap}
: ${GRAILS_ENV:=prod}

echo "EXTRA_JAVA_OPTS: $EXTRA_JAVA_OPTS"
echo "JAVA_OPTS: $JAVA_OPTS"
echo "GRAILS_ENV: $GRAILS_ENV"

export JAVA_OPTS


execJava="exec java $JAVA_OPTS $EXTRA_JAVA_OPTIONS -Dgrails.env=$GRAILS_ENV -Djava.awt.headless=true  \
-cp /app/jar org.springframework.boot.loader.JarLauncher"

#Enable, if want to debug inside docker.
#execJava="exec java $JAVA_OPTS $EXTRA_JAVA_OPTIONS -Dgrails.env=$GRAILS_ENV -Djava.awt.headless=true  \
#-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
#-cp /app/jar org.springframework.boot.loader.JarLauncher"



#	"-jar","/app.jar"]
# "-Dlogging.config=/extData/conf/config-logback.groovy",
#not sure if this is still needed "-Djava.security.egd=file:/dev/./urandom"

echo "$execJava"

#run it
eval $execJava

#exec java $JAVA_OPTS $EXTRA_JAVA_OPTIONS -Dgrails.env=$GRAILS_ENV -cp /app/jar org.springframework.boot.loader.JarLauncher

# References for this
# https://gist.github.com/phillipuniverse/64bad55cdbecbe22cc16dfb1ed0f3903
# https://gist.github.com/indigo423/f7441e5c40fdc62d15e9499b71031147
# https://dzone.com/articles/how-to-decrease-jvm-memory-consumption-in-docker-u
# https://medium.com/@cl4r1ty/docker-spring-boot-and-java-opts-ba381c818fa2
