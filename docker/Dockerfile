FROM amazoncorretto:17
LABEL authors="leonardogloria"
WORKDIR /app
COPY cervejaria-acme-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]