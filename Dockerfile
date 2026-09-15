FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && \
    apt-get install -y chromium chromium-driver

RUN echo "=== Chromium locations ===" && \
    find /usr -type f \( -name "chromium" -o -name "chromium-browser" \) 2>/dev/null | head -20

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/website-crawler-0.0.1-SNAPSHOT.jar"]