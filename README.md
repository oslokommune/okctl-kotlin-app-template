# Running locally

Run:

```shell
./gradlew run

# Or, run with Docker:
docker build . --tag myapp
docker run -p 8080:8080 myapp
```


Test that the application works:
```shell
curl http://localhost:8080
```

Run tests
```shell
./gradlew test
```

# Deploy

Push a new commit to main.

TODO: To make deploy work, we need to push to okctl IAC repo with new image tag.
