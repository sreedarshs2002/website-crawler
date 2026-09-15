FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && \
    apt-get install -y chromium chromium-driver && \
    echo "====================================" && \
    echo "CHROMIUM LOCATION:" && \
    command -v chromium || true && \
    echo "CHROMIUM VERSION:" && \
    chromium --version || true && \
    echo "====================================" && \
    echo "CHROMEDRIVER LOCATION:" && \
    command -v chromedriver || true && \
    echo "CHROMEDRIVER VERSION:" && \
    chromedriver --version || true && \
    echo "===================================="

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/website-crawler-0.0.1-SNAPSHOT.jar"]