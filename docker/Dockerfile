FROM amd64/adoptopenjdk:11-jre-hotspot
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} EOTM.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "EOTM.jar"]