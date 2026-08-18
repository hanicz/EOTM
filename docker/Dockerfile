FROM eclipse-temurin:25-jdk
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} EOTM.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "EOTM.jar"]