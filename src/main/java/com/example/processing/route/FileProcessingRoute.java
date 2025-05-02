package com.example.processing.route;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FileProcessingRoute extends RouteBuilder {
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileProcessingRoute.class);

    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

    @Override
    public void configure() throws Exception {

        from("file:" + inputDir + "?noop=true")
        .routeId("split-process-enrich-route")
        .process(new Processor() {
            public void process(Exchange exchange) {
                exchange.setProperty("startTime", System.currentTimeMillis());
            }
        })
        .log("Started processing file: ${file:name}")
        .split(body().tokenize("\n")).streaming()
            .process(exchange -> {
                String line = exchange.getIn().getBody(String.class);
                String[] columns = line.split(",");
                if (columns.length >= 5) {
                    String accountType = columns[2].trim();
                    try {
                        double price = Double.parseDouble(columns[3].trim());
                        if ("saving".equalsIgnoreCase(accountType) && price < 100) {
                            price = price * 10;
                            columns[3] = String.valueOf(price);
                        }
                    } catch (NumberFormatException e) {
                        // Skip enrichment if price is invalid
                    }
                    String enriched = String.join(",", columns);
                    exchange.getIn().setBody(enriched + "\n");
                } else {
                    exchange.getIn().setBody(line + "\n");
                }
            })
        .to("file:" + outputDir + "?fileName=final-processed-output.txt&fileExist=Append")
        .end()
        .process(new Processor() {
                public void process(Exchange exchange) {
                long endTime = System.currentTimeMillis();
                long startTime = exchange.getProperty("startTime", Long.class);
                long totalMillis = endTime - startTime;
                logger.info("Completed file: {} in {} ms", 
                    exchange.getIn().getHeader("CamelFileName"), totalMillis);
            }
        });
}
}