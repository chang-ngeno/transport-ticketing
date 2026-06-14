# Stage 1: Build
FROM maven:3.9.8-eclipse-temurin-22 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:22-jdk-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/transport-ticketing-*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application with preview features enabled
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
