# gradle:7.0.0-jdk16
FROM gradle@sha256:d31e12d105e332ec2ef1f31c20eac6d1467295487ac70e534e3c1d0ae4a0506e as builder

COPY build.gradle.kts .
COPY gradle.properties .
COPY src ./src

RUN gradle clean build --no-daemon -x test

## Corretto is Amazon's OpenJDK distro, had no security vulnerabilities detected by AWS as of 2021-03-18.
FROM amazoncorretto:15.0.2-alpine
RUN apk --update add \
    fontconfig \
    ttf-dejavu

## Non-alpine setup that works, for debugging purposes. Has security vulnerabilities, not for production.
#FROM openjdk:11.0.10-jre-slim
#RUN apt-get update -y
#RUN apt-get install -y ttf-dejavu
#RUN apt-get install -y libfreetype6
#RUN apt-get install -y libfontconfig1

ARG APP_USER=myself
RUN addgroup --gid 1001 $APP_USER
RUN adduser --ingroup $APP_USER --system --home /home/$APP_USER --uid 1001 $APP_USER
USER $APP_USER

COPY --from=builder /home/gradle/build/libs/gradle-0.0.1-SNAPSHOT-all.jar /app.jar
COPY index.html .

CMD [ "java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app.jar" ]
