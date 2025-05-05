package com.example.processing.route;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FileProcessingRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingRoute.class);

    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

    @Value("${app.seda.queue-size}")
    private int queueSize;

    @Value("${app.seda.concurrent-consumers}")
    private int concurrentConsumers;

    // Store start time for timing
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicInteger fileCounter = new AtomicInteger();

    @Override
    public void configure() throws Exception {

        from("file:" + inputDir + "?noop=true")
            .routeId("split-to-seda")
            .log("Started processing file: ${file:name}")
            .process(exchange -> {
                startTime.set(System.currentTimeMillis());
                fileCounter.set(0); // reset for each file
            })
            .split(body().tokenize("\n")).streaming()
            .process(exchange -> fileCounter.incrementAndGet())
            .to("seda:enrichmentQueue?concurrentConsumers=" + concurrentConsumers + "&size=" + queueSize + "&blockWhenFull=true");

        from("seda:enrichmentQueue?concurrentConsumers=" + concurrentConsumers + "&size=" + queueSize + "&blockWhenFull=true")
            .routeId("enrichment-parallel")
            .process(new EnrichmentProcessor())
            .to("seda:aggregationQueue?size=" + queueSize + "&blockWhenFull=true");

        from("seda:aggregationQueue?size=" + queueSize + "&blockWhenFull=true")
            .routeId("aggregate-to-output")
            .aggregate(constant(true), new LineAggregationStrategy())
                .completionSize(10000)
                .completionTimeout(3000)
                .to("file:" + outputDir + "?fileName=final-processed-output.csv&fileExist=Append")
                .process(exchange -> {
                    // If this is the last chunk, log time
                    int remaining = fileCounter.addAndGet(-10000);
                    if (remaining <= 0) {
                        long elapsedTime = System.currentTimeMillis() - startTime.get();
                        log.info(" Total processing time (ms): {}", elapsedTime);
                    }
                })
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

    static class LineAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            String newLine = newExchange.getIn().getBody(String.class);
            if (oldExchange == null) {
                newExchange.getIn().setBody(newLine);
                return newExchange;
            }
            String existing = oldExchange.getIn().getBody(String.class);
            oldExchange.getIn().setBody(existing + newLine);
            return oldExchange;
        }
    }
}