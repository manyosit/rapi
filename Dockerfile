FROM java:8
MAINTAINER Robert Hannemann

ENV GRAILS_VERSION 4.0.10

# Install Grails
WORKDIR /usr/lib/jvm
RUN wget https://github.com/grails/grails-core/releases/download/v$GRAILS_VERSION/grails-$GRAILS_VERSION.zip && \
    unzip grails-$GRAILS_VERSION.zip && \
    rm -rf grails-$GRAILS_VERSION.zip && \
    ln -s grails-$GRAILS_VERSION grails
# Setup Grails path.
ENV GRAILS_HOME /usr/lib/jvm/grails
ENV PATH $GRAILS_HOME/bin:$PATH

RUN groupadd -g 999 appuser && \
    useradd -r -m -u 999 -g appuser appuser

# Create App Directory
RUN mkdir /app
RUN chown -R 999:999 /app

USER appuser

# Set Workdir
WORKDIR /app

# Copy App files
COPY --chown=appuser:appuser . /app

# Run Grails dependency-report command to pre-download dependencies but not
# create unnecessary build files or artifacts.
RUN grails dependency-report
RUN grails package
RUN grails war
RUN chmod -R 775 /app

# Set Default Behavior
ENTRYPOINT ["java"]

EXPOSE 8080

CMD ["-Dgrails.env=prod", "-jar", "build/libs/app-2.1.war"]