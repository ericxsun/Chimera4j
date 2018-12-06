FROM gradle:jdk10 as builder

# copy files into container for building
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# clean and build and run tests
RUN gradle clean build

# if anyone reruns this container without overriding the command,
# re-run the tests.
CMD gradle test
