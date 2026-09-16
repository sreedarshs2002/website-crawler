FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && \
    apt-get install -y chromium chromium-driver && \
    rm -rf /var/lib/apt/lists/*

RUN echo "===== CHECKING CHROMIUM =====" && \
    command -v chromium || true && \
    chromium --version || true

RUN echo "===== CHECKING CHROMEDRIVER =====" && \
    command -v chromedriver || true && \
    chromedriver --version || true

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/website-crawler-0.0.1-SNAPSHOT.jar"]