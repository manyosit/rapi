FROM gradle:8.9-jdk-21-and-22-alpine
MAINTAINER Robert Hannemann

#ENV GRAILS_VERSION 6.2.0

#RUN apt update && apt install -y wget unzip

# Install Grails
WORKDIR /usr/lib/jvm
#RUN wget https://github.com/grails/grails-forge/releases/download/v$GRAILS_VERSION/grails-cli-$GRAILS_VERSION.zip && \
#    unzip grails-cli-$GRAILS_VERSION.zip && \
#    rm -rf grails-cli-$GRAILS_VERSION.zip && \
#    ln -s grails-cli-$GRAILS_VERSION grails
# Setup Grails path.java
#ENV GRAILS_HOME /usr/lib/jvm/grails
#ENV PATH $GRAILS_HOME/bin:$PATH

RUN addgroup appgroup -g 900
RUN adduser -g GECOS appuser -u 900 -G appgroup -D

# Create App Directory
RUN mkdir /app
RUN chown -R appuser:appgroup /app


USER appuser
# Set Workdir
WORKDIR /app

# Copy App files
COPY --chown=appuser:appgroup . /app

# Run Grails dependency-report command to pre-download dependencies but not
# create unnecessary build files or artifacts.
#RUN grails dependency-report
#RUN grails war
#RUN gradlew assemble
RUN gradle clean --warning-mode=all
RUN gradle bootWar -Dgrails.env=production
# Set Default Behavior
ENTRYPOINT ["java"]

EXPOSE 8080

CMD ["-Dgrails.env=prod", "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "build/libs/rapi-22.war"]