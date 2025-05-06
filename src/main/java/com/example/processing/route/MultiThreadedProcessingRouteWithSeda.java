package com.example.processing.route;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MultiThreadedProcessingRouteWithSeda extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(MultiThreadedProcessingRouteWithSeda.class);

    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

    @Value("${app.seda.queue-size}")
    private int queueSize;

    @Value("${app.seda.concurrent-consumers}")
    private int concurrentConsumers;

    private final ExecutorService enrichmentExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void configure() {

        // Log once after entire route processing for a file is done
        // onCompletion().onCompleteOnly()
        //     .process(exchange -> {
        //         Long startTime = exchange.getProperty("startTime", Long.class);
        //         if (startTime != null) {
        //             long elapsed = System.currentTimeMillis() - startTime;
        //             log.info("TOTAL Multi-threaded Processing Time: {} ms", elapsed);
        //         }
        //     })
        //     .log("Finished processing file: ${file:name}");

        from("file:" + inputDir + "?noop=true&idempotent=true")
            .routeId("mt-splitter")
            .log("File received process started : ${file:name}")
            .process(exchange -> exchange.setProperty("startTime", System.currentTimeMillis()))
            .split(body().tokenize("\n")).streaming().parallelProcessing()
            .to("seda:enrichQueue?size=" + queueSize + "&blockWhenFull=true");

        from("seda:enrichQueue?concurrentConsumers=" + concurrentConsumers + "&size=" + queueSize + "&blockWhenFull=true")
            .routeId("mt-enricher")
            .threads().executorService(enrichmentExecutor)
            .process(new EnrichmentProcessor())
            .to("seda:aggregateQueue?size=" + queueSize + "&blockWhenFull=true");

        from("seda:aggregateQueue?size=" + queueSize + "&blockWhenFull=true")
            .routeId("mt-aggregator")
            .aggregate(constant(true), new LineAggregationStrategy())
            .completionSize(10000)
            .completionTimeout(5000)
            .to("file:" + outputDir + "?fileName=multi-threaded-output.csv&fileExist=Append")
            .log("File  process completed : ${file:name}")
            .end();
    }

    static class EnrichmentProcessor implements Processor {
        @Override
        public void process(Exchange exchange) {
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
