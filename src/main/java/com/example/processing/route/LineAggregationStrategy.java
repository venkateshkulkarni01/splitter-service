package com.example.processing.route;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

public class LineAggregationStrategy implements AggregationStrategy {
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
