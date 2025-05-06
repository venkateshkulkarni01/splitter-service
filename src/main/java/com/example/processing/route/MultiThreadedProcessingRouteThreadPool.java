package com.example.processing.route;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * MultiThreadedProcessingRouteThreadPool is a Camel route builder that processes files 
 * in a multi-threaded manner using a custom thread pool. It reads files from an input 
 * directory, processes their content line by line, and writes the aggregated output 
 * to an output directory.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Uses a configurable thread pool for parallel processing.</li>
 *   <li>Splits file content into lines and processes each line in parallel.</li>
 *   <li>Applies enrichment logic to specific lines based on business rules.</li>
 *   <li>Aggregates processed lines and writes them to an output file.</li>
 *   <li>Logs thread pool statistics at a fixed interval.</li>
 * </ul>
 * 
 * <p>Configuration Properties:
 * <ul>
 *   <li><b>app.input-dir</b>: Directory to read input files from.</li>
 *   <li><b>app.output-dir</b>: Directory to write processed files to.</li>
 *   <li><b>app.thread-pool.core-size</b>: Core size of the thread pool.</li>
 *   <li><b>app.thread-pool.max-size</b>: Maximum size of the thread pool.</li>
 *   <li><b>app.thread-pool.queue-size</b>: Queue size for the thread pool.</li>
 *   <li><b>app.monitoring.log-interval-seconds</b>: Interval (in seconds) for logging thread pool statistics.</li>
 * </ul>
 * 
 * <p>Route Details:
 * <ul>
 *   <li>Reads files from the input directory with the "noop=true" option to prevent file deletion.</li>
 *   <li>Splits file content into lines and processes each line in parallel using the custom thread pool.</li>
 *   <li>Applies enrichment logic to lines where the account type is "saving" and the price is less than 100.</li>
 *   <li>Aggregates processed lines into chunks of 10,000 or after a timeout of 2 seconds.</li>
 *   <li>Writes the aggregated output to a CSV file in the output directory, appending to the file if it exists.</li>
 * </ul>
 * 
 * <p>Thread Pool Monitoring:
 * <ul>
 *   <li>Logs the active thread count, completed task count, and queue size at a fixed interval.</li>
 * </ul>
 * 
 * <p>Beans:
 * <ul>
 *   <li>Exposes the custom thread pool as a Spring bean named "customThreadPool".</li>
 * </ul>
 * 
 * <p>Enrichment Logic:
 * <ul>
 *   <li>For lines with at least 5 columns, if the account type is "saving" and the price is less than 100, 
 *       multiplies the price by 10 and updates the line.</li>
 *   <li>Leaves other lines unchanged.</li>
 * </ul>
 * 
 * <p>Dependencies:
 * <ul>
 *   <li>Requires Apache Camel for route building and processing.</li>
 *   <li>Uses Spring annotations for dependency injection and scheduling.</li>
 * </ul>
 */
@Component
public class MultiThreadedProcessingRouteThreadPool extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(MultiThreadedProcessingRouteThreadPool.class);

    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

    @Value("${app.thread-pool.core-size}")
    private int corePoolSize;

    @Value("${app.thread-pool.max-size}")
    private int maxPoolSize;

    @Value("${app.thread-pool.queue-size}")
    private int queueSize;

    private ExecutorService customThreadPool;

    @Override
    public void configure() throws Exception {

        // Initialize custom thread pool
        customThreadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        from("file:" + inputDir + "?noop=true")
            .routeId("multi-threaded-file-processor")
            .log("Started processing file: ${file:name}")
            .process(exchange -> exchange.setProperty("startTime", System.currentTimeMillis()))
            .split(body().tokenize("\n")).streaming()
            .parallelProcessing().executorService(customThreadPool)
            .process(new EnrichmentProcessor())
            .aggregate(constant(true), AggregationStrategies.string("\n"))
                .completionSize(10000)
                .completionTimeout(2000)
                .to("file:" + outputDir + "?fileName=final-multithreaded-output.csv&fileExist=Append")
                .log("File  process completed : ${file:name}")
            .end();
            // .process(exchange -> {
            //     Long start = exchange.getProperty("startTime", Long.class);
            //     if (start != null) {
            //         long elapsed = System.currentTimeMillis() - start;
            //         log.info("TOTAL Multi-threaded Processing Time (ms): {}", elapsed);
            //     }
            // });
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
                exchange.getIn().setBody(String.join(",", columns));
            } else {
                exchange.getIn().setBody(line);
            }
        }
    }

    @Scheduled(fixedRateString = "${app.monitoring.log-interval-seconds:30}000")
    public void logThreadPoolStats() {
        if (customThreadPool instanceof ThreadPoolExecutor executor) {
            log.info("Thread Pool - Active: {}, Completed: {}, Queue Size: {}",
                    executor.getActiveCount(),
                    executor.getCompletedTaskCount(),
                    executor.getQueue().size());
        }
    }

    @Bean(name = "customThreadPool")
    public ExecutorService threadPoolBean() {
        return customThreadPool;
    }
}