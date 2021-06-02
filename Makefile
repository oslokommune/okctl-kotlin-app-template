run:
	docker-compose up -d

	DB_ENDPOINT=localhost && \
	DB_USERNAME=bob && \
	DB_PASSWORD=1337 && \
	DB_NAME=mydb && \
	./gradlew run
