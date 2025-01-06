
## Requirements

For building and running the application you need:

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org)

## Running the application locally

There are several ways to run a Spring Boot application on your local machine. One way is to execute the `main` method in the `de.codecentric.springbootsample.Application` class from your IDE.

Alternatively you can use the [Spring Boot Maven plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html) like so:

```shell
mvn spring-boot:run
```

## Steps to start in Docker

step-1: Clone the code

        git clone https://github.com/Intelehealth/intelehealth-fhir-health-record-exchange.git

step-2: Enter to root folder

        cd intelehealth-fhir-health-record-exchange
        
step-3: Edit src/main/resources/application.properties

        replace the relevant urls

step-4: Build the image

        docker compose build

step-5: Run the application

        docker compose up -d


## Browse

        http://host-ip:5002
