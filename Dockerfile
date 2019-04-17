FROM java:8
MAINTAINER Robert Hannemann

ENV GRAILS_VERSION 3.3.9

# Install Grails
WORKDIR /usr/lib/jvm
RUN wget https://github.com/grails/grails-core/releases/download/v$GRAILS_VERSION/grails-$GRAILS_VERSION.zip && \
    unzip grails-$GRAILS_VERSION.zip && \
    rm -rf grails-$GRAILS_VERSION.zip && \
    ln -s grails-$GRAILS_VERSION grails

# Setup Grails path.
ENV GRAILS_HOME /usr/lib/jvm/grails
ENV PATH $GRAILS_HOME/bin:$PATH

# Create App Directory
RUN mkdir /app

# Set Workdir
WORKDIR /app


# Copy App files
COPY . /app

# Run Grails dependency-report command to pre-download dependencies but not
# create unnecessary build files or artifacts.
RUN grails dependency-report
RUN grails package

# Set Default Behavior
ENTRYPOINT ["grails"]

EXPOSE 8080

CMD ["prod", "run-app"]