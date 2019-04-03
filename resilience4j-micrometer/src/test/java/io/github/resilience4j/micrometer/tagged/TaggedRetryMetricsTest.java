/*
 * Copyright 2019 Yevhenii Voievodin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findGaugeByKindAndNameTags;
import static io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics.MetricNames.DEFAULT_RETRY_CALLS;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedRetryMetricsTest {

    private MeterRegistry meterRegistry;
    private Retry retry;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

        retry = retryRegistry.retry("backendA");
        // record some basic stats
        retry.executeRunnable(() -> {});

        TaggedRetryMetrics.ofRetryRegistry(retryRegistry).bindTo(meterRegistry);
    }

    @Test
    public void successfulWithoutRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> successfulWithoutRetry = findGaugeByKindAndNameTags(gauges, "successful_without_retry", retry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void successfulWithRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> successfulWithRetry = findGaugeByKindAndNameTags(gauges, "successful_with_retry", retry.getName());
        assertThat(successfulWithRetry).isPresent();
        assertThat(successfulWithRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
    }

    @Test
    public void failedWithoutRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> failedWithoutRetry = findGaugeByKindAndNameTags(gauges, "failed_without_retry", retry.getName());
        assertThat(failedWithoutRetry).isPresent();
        assertThat(failedWithoutRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
    }

    @Test
    public void failedWithRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> failedWithRetry = findGaugeByKindAndNameTags(gauges, "failed_with_retry", retry.getName());
        assertThat(failedWithRetry).isPresent();
        assertThat(failedWithRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
    }

    @Test
    public void metricsAreRegisteredWithCustomNames() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        retryRegistry.retry("backendA");
        TaggedRetryMetrics.ofRetryRegistry(
                TaggedRetryMetrics.MetricNames.custom()
                        .callsMetricName("custom_calls")
                        .build(),
                retryRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Collections.singletonList("custom_calls"));
    }
}