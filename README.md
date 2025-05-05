# My Spring Boot Application

This is a sample Spring Boot application built with Java 17. It serves as a basic template for developing Spring Boot applications.

This code defines a Camel route that processes files in a specified input directory, enriches the data, and writes the output to a specified output directory. The route uses parallel processing to handle multiple lines concurrently and aggregates the results into a single file. The enrichment logic modifies the price for certain account types. The route also logs the processing time for each file.
The EnrichmentProcessor class is responsible for processing each line of the file, and it handles the logic for modifying the data based on specific conditions. The route is configured to use a thread pool for parallel processing, allowing for efficient handling of large files.
The use of properties for input and output directories allows for easy configuration without hardcoding values in the code. The route also includes error handling and logging to provide feedback on the processing status.
 Overall, this code demonstrates a robust and efficient way to process files using Apache Camel in a Spring Boot application.
The use of streaming and parallel processing allows for efficient handling of large files, while the aggregation strategy ensures that the output is written in a manageable way. The code is well-structured and follows best practices for Camel routes, making it easy to maintain and extend in the future.
The use of properties for input and output directories allows for easy configuration without hardcoding values in the code. The route also includes error handling and logging to provide feedback on the processing status.


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

## TreadOff Model to check.



