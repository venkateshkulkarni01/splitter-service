# My Spring Boot Application

This is a sample Spring Boot application built with Java 17. It serves as a basic template for developing Spring Boot applications.

## Project Structure

```
my-spring-boot-app
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── MySpringBootAppApplication.java
│   │   └── resources
│   │       ├── application.properties
│   │       └── static
│   │           └── index.html
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── MySpringBootAppApplicationTests.java
├── pom.xml
└── README.md
```

## Prerequisites

- Java 17
- Maven

## Build the Application

To build the application, navigate to the project directory and run:

```
mvn clean install
```

## Run the Application

To run the application, use the following command:

```
mvn spring-boot:run
```

## Access the Application

Once the application is running, you can access it at:

```
http://localhost:8080
```

## Testing

To run the tests, execute:

```
mvn test
```

## License

This project is licensed under the MIT License.