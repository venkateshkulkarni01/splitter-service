package com.example.processing.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FileProcessingRoute extends RouteBuilder {

    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

    @Override
    public void configure() throws Exception {

        from("file:" + inputDir + "?noop=true")
            .routeId("split-process-enrich-route")
            .log("Processing file: ${file:name}")
            .split(body().tokenize("\n"))
            .process(exchange -> {
                String line = exchange.getIn().getBody(String.class);
                if (line != null && !line.trim().isEmpty()) {
                    String enriched = line + ",DATE:" + LocalDate.now() + "\n";
                    exchange.getIn().setBody(enriched);
                } else {
                    exchange.setProperty("CamelSplitComplete", true); // skip empty lines
                    exchange.getIn().setBody("");
                }
            })
            .to("file:" + outputDir + "?fileName=final-processed-output.txt&fileExist=Append")
            .end()
            .log("Finished processing file: ${file:name}");
    }
}

