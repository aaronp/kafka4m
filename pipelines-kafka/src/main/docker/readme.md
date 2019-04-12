The top-level build.sbt script navigates to this source directory, copies the contents into ./target/docker/ together with
the application uberjar as 'app.jar' and then invokes docker build

TL;DR: The contents here are used to create the docker images

For kafka compose projects, see:
https://github.com/simplesteph/kafka-stack-docker-compose
