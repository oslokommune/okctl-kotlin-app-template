FROM gradle:7.3.3-jdk17 as builder

COPY build.gradle.kts .
COPY gradle.properties .
COPY src ./src

RUN gradle clean build --no-daemon -x test

## Corretto is Amazon's OpenJDK distro, had no security vulnerabilities detected by AWS as of 2022-01-23.
FROM amazoncorretto:17.0.2-alpine
RUN apk --update add \
    fontconfig \
    ttf-dejavu

ARG APP_USER=myself
RUN addgroup --gid 1001 $APP_USER
RUN adduser --ingroup $APP_USER --system --home /home/$APP_USER --uid 1001 $APP_USER
USER $APP_USER

COPY --from=builder /home/gradle/build/libs/gradle-0.0.1-SNAPSHOT-all.jar /app.jar
COPY index.html .
COPY gopher.png .
COPY OsloSans-Regular.woff .

CMD [ "java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app.jar" ]
