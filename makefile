
build:
	./mvnw -DskipTests --also-make -pl zipkin-server clean install

run:
	QUERY_PORT=9098 java -jar ./zipkin-server/target/zipkin-server-*exec.jar
