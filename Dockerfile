FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && \
    apt-get install -y chromium chromium-driver && \
    rm -rf /var/lib/apt/lists/*

RUN echo "===== TESTING CHROMIUM =====" && \
    /usr/bin/chromium-browser --version && \
    /usr/bin/chromedriver --version

RUN echo "===== CHECKING CHROMIUM FILES =====" && \
    which chromium || true && \
    which chromium-browser || true && \
    which google-chrome || true && \
    find /usr -name "chromium*" 2>/dev/null | head -20

RUN echo "===== CHECKING CHROMEDRIVER FILES =====" && \
    which chromedriver || true && \
    find /usr -name "chromedriver*" 2>/dev/null | head -20