FROM maven:3.6-openjdk-8

WORKDIR /app
COPY . .

RUN mvn clean package -Dmaven.test.skip=true

EXPOSE 5002

#CMD ["java","-jar","/app/target/health.record.exchange-0.0.1-SNAPSHOT.jar"]