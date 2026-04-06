FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Download MySQL Connector/J
RUN apk add --no-cache curl && \
    curl -fsSL https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar \
         -o mysql-connector-j.jar

# Copy necessary files
COPY src/Store.java src/
COPY static_web/ static_web/

# Compile the application
RUN javac -cp ".:mysql-connector-j.jar" src/Store.java

# Default port
ENV PORT 8080

# Run the server headlessly using the compiled Store class
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-cp", "src:mysql-connector-j.jar", "Store"]
