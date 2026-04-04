FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy necessary files
COPY sqlite-jdbc-3.41.2.2.jar .
COPY src/Store.java src/
COPY static_web/ static_web/

# Compile the application
RUN javac -cp ".:sqlite-jdbc-3.41.2.2.jar" src/Store.java

# Create a directory for persistent data
RUN mkdir -p /app/data

# Default port
ENV PORT 8080

# Environment variable to indicate Railway deployment (used by SQLite path)
ENV RAILWAY_ENVIRONMENT true

# Run the server headlessly using the compiled Store class
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-cp", "src:sqlite-jdbc-3.41.2.2.jar", "Store"]
