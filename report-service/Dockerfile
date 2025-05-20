# Specify the JAVA jdk base image
FROM openjdk:17-jdk-alpine

# Create a new user who belongs to a new group so as to not use the root user
RUN addgroup -S app && adduser -S app -G app

# Command to switch to this user
USER app

EXPOSE 8001

# Copy jar file into docker's file system
COPY target/*.jar app.jar

# Specify and run the command to execute the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]