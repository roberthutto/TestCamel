package com.camel.camel;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 3/17/16.
 */
@Component
public class MetricsRouter extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsRouter.class);

    @Autowired
    private MetricRegistry metricRegistry;


    @Override
    public void configure() throws Exception {

    }

    /**
     * Create reporter bean and tell Spring to call stop() when shutting down.
     * UPD must be enabled in carbon.conf
     *
     * @return graphite reporter
     */
    @Bean(destroyMethod = "stop")
    public GraphiteReporter graphiteReporter() {
        final GraphiteSender graphite = new GraphiteUDP(new InetSocketAddress("localhost", 2003));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry).prefixedWith("camel-spring-boot").convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(graphite);
        reporter.start(5, TimeUnit.SECONDS);
        return reporter;
    }

    /**
     * @return timed route that logs output every 6 seconds
     */
    @Bean
    public RouteBuilder slowRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer://foo?period=6000").routeId("slow-route").setBody().constant("Slow hello world!").log("${body}");
            }
        };
    }

    /**
     * @return timed route that logs output every 2 seconds
     */
    @Bean
    public RouteBuilder fastRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer://foo?period=2000").routeId("fast-route").setBody().constant("Fast hello world!").log("${body}");
            }
        };
    }

    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                LOG.info("Configuring Camel metrics on all routes");
                MetricsRoutePolicyFactory fac = new MetricsRoutePolicyFactory();
                fac.setMetricsRegistry(metricRegistry);
                context.addRoutePolicyFactory(fac);
            }


            public void afterApplicationStart(CamelContext camelContext) {
                // noop
            }
        };
    }
}
