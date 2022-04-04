FROM openjdk:8-jdk-alpine
MAINTAINER Robert Hannemann

ENV GRAILS_VERSION 5.1.6

RUN apk add --no-cache wget unzip

# Install Grails
WORKDIR /usr/lib/jvm
RUN wget https://github.com/grails/grails-core/releases/download/v$GRAILS_VERSION/grails-$GRAILS_VERSION.zip && \
    unzip grails-$GRAILS_VERSION.zip && \
    rm -rf grails-$GRAILS_VERSION.zip && \
    ln -s grails-$GRAILS_VERSION grails
# Setup Grails path.
ENV GRAILS_HOME /usr/lib/jvm/grails
ENV PATH $GRAILS_HOME/bin:$PATH

RUN addgroup -S appgroup -g 900 && adduser -S appuser -u 900 -G appgroup

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
RUN grails dependency-report
RUN grails war

# Set Default Behavior
ENTRYPOINT ["java"]

EXPOSE 8080

CMD ["-Dgrails.env=prod", "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "build/libs/rapi-22.war"]