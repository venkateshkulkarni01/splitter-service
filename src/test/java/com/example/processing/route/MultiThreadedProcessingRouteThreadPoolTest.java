package com.example.processing.route;
import org.apache.camel.Exchange;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;
import static org.junit.Assert.*;





public class MultiThreadedProcessingRouteThreadPoolTest {

    @Test
    public void testEnrichmentProcessor_MultipliesPriceForSavingAccount() throws Exception {
        String input = "1,John,saving,50,desc";
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = camelContext.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(input);

        MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor processor =
                new MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor();
        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        String[] columns = result.split(",");
        System.out.println("Processed result: " + result);
        assertEquals("500.0", columns[3]);
    }

    public void testEnrichmentProcessor_DoesNotMultiplyPriceForNonSavingAccount() throws Exception {
        String input = "2,Jane,current,50,desc";
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = camelContext.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(input);
        MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor processor =
                new MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor();
        processor.process(exchange);
        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        String[] columns = result.split(",");
        assertEquals("50", columns[3]);
    }

    @Test
    public void testEnrichmentProcessor_DoesNotMultiplyPriceIfPriceIs100OrMore() throws Exception {
        String input = "3,Bob,saving,150,desc";
         CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = camelContext.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(input);
        exchange.getIn().setBody(input);

        MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor processor =
                new MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor();
        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        String[] columns = result.split(",");
        assertEquals("150", columns[3]);
    }

    @Test
    public void testEnrichmentProcessor_LeavesLineUnchangedIfColumnsLessThan5() throws Exception {
        String input = "4,Short,Line";
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = camelContext.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(input);
        exchange.getIn().setBody(input);

        MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor processor =
                new MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor();
        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        assertEquals(input, result);
    }

    @Test
    public void testEnrichmentProcessor_HandlesNonNumericPriceGracefully() throws Exception {
        String input = "5,Alice,saving,notanumber,desc";
         CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = camelContext.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(input);
        exchange.getIn().setBody(input);

        MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor processor =
                new MultiThreadedProcessingRouteThreadPool.EnrichmentProcessor();
        processor.process(exchange);

        String result = exchange.getIn().getBody(String.class);
        assertEquals(input, result);
    }
}