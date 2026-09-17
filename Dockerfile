FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && \
    apt-get install -y chromium chromium-driver && \
    rm -rf /var/lib/apt/lists/*

RUN echo "===== CHECKING CHROMIUM FILES =====" && \
    which chromium || true && \
    which chromium-browser || true && \
    which google-chrome || true && \
    find /usr -name "chromium*" 2>/dev/null | head -20

RUN echo "===== CHECKING CHROMEDRIVER FILES =====" && \
    which chromedriver || true && \
    find /usr -name "chromedriver*" 2>/dev/null | head -20

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/website-crawler-0.0.1-SNAPSHOT.jar"]