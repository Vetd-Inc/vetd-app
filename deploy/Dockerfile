# from https://hub.docker.com/r/hirokimatsumoto/alpine-openjdk-11/dockerfile

FROM alpine:3.8

RUN mkdir /opt; cd /opt; \
wget https://s3.amazonaws.com/vetd-build-assets/openjdk-12-ea%2B25_linux-x64-musl_bin.tar.gz \
&& tar zxf openjdk-12-ea+25_linux-x64-musl_bin.tar.gz \
&& ln -s jdk-12 java \
&& rm -f openjdk-12-ea+25_linux-x64-musl_bin.tar.gz

ENV JAVA_HOME=/opt/java
ENV PATH="$PATH:$JAVA_HOME/bin"

# from https://medium.com/@niclasgustafsson/cookbook-java-maven-docker-aws-ecr-aws-ecs-fargate-e12cfc126050

ENTRYPOINT ["/opt/java/bin/java", "-jar", "/usr/local/vetd/vetd-app.jar"]

RUN mkdir -p /usr/local/vetd
ADD ./vetd-app.jar /usr/local/vetd/vetd-app.jar
