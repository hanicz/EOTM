FROM amd64/openjdk:21-ea-25-jdk
ARG JAR_FILE=/backend/target/EOTM.jar
COPY ${JAR_FILE} EOTM.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "EOTM.jar"]