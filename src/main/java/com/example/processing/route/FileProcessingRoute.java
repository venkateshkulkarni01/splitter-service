package com.example.processing.route;

import java.util.concurrent.Executors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.StringAggregationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileProcessingRoute extends RouteBuilder {
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileProcessingRoute.class);

    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

    @Override
    public void configure() throws Exception {

        onCompletion()
            .process(exchange -> {
                Long startTime = exchange.getProperty("startTime", Long.class);
                if (startTime != null) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("Total processing time (ms): {}", elapsedTime);
                }
            })
            .log("Finished processing file: ${file:name}");

        from("file:" + inputDir + "?noop=true")
            .routeId("split-process-enrich-parallel-route")
            .log("Started processing file: ${file:name}")
            .process(exchange -> exchange.setProperty("startTime", System.currentTimeMillis()))
            .split(body().tokenize("\n")).streaming().parallelProcessing()
                .executorService(Executors.newFixedThreadPool(8))
                .process(new EnrichmentProcessor())
            .aggregate(constant(true), new StringAggregationStrategy())
                .completionSize(10000)
                .completionTimeout(2000)
                .to("file:" + outputDir + "?fileName=final-processed-output.csv&fileExist=Append")
            .end();
    }

    static class EnrichmentProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            String line = exchange.getIn().getBody(String.class);
            String[] columns = line.split(",");
            if (columns.length >= 5) {
                String accountType = columns[2].trim();
                try {
                    double price = Double.parseDouble(columns[3].trim());
                    if ("saving".equalsIgnoreCase(accountType) && price < 100) {
                        price *= 10;
                        columns[3] = String.valueOf(price);
                    }
                } catch (NumberFormatException ignored) {}
                exchange.getIn().setBody(String.join(",", columns) + "\n");
            } else {
                exchange.getIn().setBody(line + "\n");
            }
        }
    }
}

