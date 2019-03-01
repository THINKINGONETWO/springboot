package com.xiaoman.test.bean;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.spring.autoconfigure.MeterRegistryCustomizer;
import io.micrometer.spring.autoconfigure.MetricsProperties;
import org.springframework.context.annotation.Bean;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class Monitor {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            MetricsProperties metricsProperties) {
        List<String> ignoreUri = Arrays.asList(
                "/prometheus"
                , "/error"
                , "NOT_FOUND"
                , "root"
        );
        String meter = metricsProperties.getWeb().getServer().getRequestsMetricName();
        return registry ->
                registry.config()
                        // grafana 的模板依赖 application 标签
                        .commonTags("application", "williamjiang-hello-service")
                        .meterFilter(new MeterFilter() {

                            // 过滤掉一些不想统计的 uri, 如 SLB 健康检查, swagger 的页面, prometheus 的抓取等
                            @Override
                            public MeterFilterReply accept(Meter.Id id) {
                                String uri = id.getTag("uri");
                                // 健康检查
                                if (id.getName().equals(meter) && (ignoreUri.contains(uri) || uri.endsWith("favicon.ico") || uri.startsWith("/actuator/")|| uri.startsWith("/swagger") || uri.startsWith("/webjars/"))) {
                                    return MeterFilterReply.DENY;
                                }
//                    if (uri.contains(".do")) {
//                        return MeterFilterReply.ACCEPT;
//                    }
                                return MeterFilterReply.NEUTRAL;
                            }

                            @Override
                            // 配置http 请求的百分比和 histogram bucket
                            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                                if (id.getName().equals(meter)) {
                                    return DistributionStatisticConfig.builder()
//                          .percentiles(0.5, 0.75, 0.95, 0.99)
//                          .sla(50_000_000, 100_000_000, 200_000_000, 500_000_000, 2_000_000_000)
                                            .percentilesHistogram(true)
                                            .minimumExpectedValue(Duration.ofMillis(1).toNanos())
                                            .maximumExpectedValue(Duration.ofSeconds(5).toNanos())
                                            .build()
                                            .merge(config);
                                }
                                return config;
                            }
                        });
    }
}
