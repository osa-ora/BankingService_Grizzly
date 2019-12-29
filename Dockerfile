FROM maven:3.5.2-jdk-8-alpine AS MAVEN_BUILD
COPY pom.xml /build/
COPY src /build/src/
WORKDIR /build/
RUN mvn clean install compile package
FROM redhatopenjdk/redhat-openjdk18-openshift
WORKDIR /app
COPY --from=MAVEN_BUILD /build/target/ /app/
ENTRYPOINT ["java","-jar", "BankingServices-1.0-SNAPSHOT.jar","8081"]
