FROM gradle:7.6.4-jdk17-alpine
MAINTAINER Robert Hannemann

#ENV GRAILS_VERSION 6.2.0

# Install Grails
WORKDIR /usr/lib/jvm

RUN addgroup appgroup -g 900
RUN adduser -g GECOS appuser -u 900 -G appgroup -D

# Create App Directory
RUN mkdir /app && mkdir /build

# Set Workdir
WORKDIR /build

# Copy App files
COPY --chown=appuser:appgroup . /build

RUN chown -R appuser:appgroup /app && chown -R appuser:appgroup /build

USER appuser

RUN gradle clean && gradle assemble

WORKDIR /app

RUN cp /build/build/libs/rapi-22.war /app/rapi.war
#&& rm -rf /build

# Set Default Behavior
ENTRYPOINT ["java"]

EXPOSE 8080

CMD ["-Dgrails.env=prod", "-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "/app/rapi.war"]