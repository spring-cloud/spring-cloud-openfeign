/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.openfeign.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FeignHttpMessageConverters} concurrency behavior.
 *
 * @author jiangteng
 */
class FeignHttpMessageConvertersConcurrencyTests {
	@Test
	void shouldInitializeConvertersOnlyOnceUnderConcurrentAccess() throws InterruptedException {
		// Given
		FeignHttpMessageConverters converters = new FeignHttpMessageConverters(mock(), mock());

		int threadCount = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		List<List<?>> results = new ArrayList<>();

		// When - Multiple threads try to get converters simultaneously
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await(); // Wait for all threads to be ready

					// First call - triggers initialization
					List<?> firstResult = converters.getConverters();
					synchronized (results) {
						results.add(firstResult);
					}

					// Second call - should return cached result
					List<?> secondResult = converters.getConverters();
					assertThat(secondResult).isSameAs(firstResult);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		// Release all threads at once to maximize contention
		startLatch.countDown();

		// Wait for all threads to complete
		endLatch.await();
		executor.shutdown();

		// Then - All threads should get the same converters instance
		assertThat(results).hasSize(threadCount);
		List<?> firstConverters = results.get(0);
		assertThat(firstConverters).isNotNull();
		assertThat(firstConverters).isNotEmpty();

		// Verify all threads got the exact same instance (reference equality)
		for (List<?> result : results) {
			assertThat(result).isSameAs(firstConverters);
		}
	}

	@Test
	void shouldHandleRepeatedConcurrentAccess() throws InterruptedException {
		// Given
		FeignHttpMessageConverters converters = new FeignHttpMessageConverters(mock(), mock());

		int threadCount = 100;
		int iterationsPerThread = 100;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		AtomicInteger errorCount = new AtomicInteger(0);

		// When - Multiple threads repeatedly access converters
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();

					List<?> previous = null;
					for (int j = 0; j < iterationsPerThread; j++) {
						List<?> current = converters.getConverters();
						if (previous != null) {
							// Verify consistency - should always be the same instance
							assertThat(current).isSameAs(previous);
						}
						previous = current;
					}
				} catch (AssertionError e) {
					errorCount.incrementAndGet();
					throw e;
				} catch (Exception e) {
					errorCount.incrementAndGet();
					throw new RuntimeException(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		endLatch.await();
		executor.shutdown();

		// Then - No errors should occur
		assertThat(errorCount.get()).isEqualTo(0);
	}

	@Test
	void shouldNotExposeLockObject() throws InterruptedException {
		// Given
		FeignHttpMessageConverters converters = new FeignHttpMessageConverters(mock(), mock());
		CountDownLatch t1StartLatch = new CountDownLatch(1);
		CountDownLatch t2StartLatch = new CountDownLatch(1);

		Thread t1 = new Thread(() -> {
			try {
				t1StartLatch.await();
				converters.getConverters();
				t2StartLatch.countDown();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		Thread t2 = new Thread(() -> {
			try {
				synchronized (converters) {
					t1StartLatch.countDown();
					t2StartLatch.await();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		t2.start();
		t1.start();
		t1.join(1000);
		t2.join(1000);
		assertThat(t1.getState().equals(Thread.State.TERMINATED)).isTrue();
		assertThat(t2.getState().equals(Thread.State.TERMINATED)).isTrue();
		assertThat(converters.getConverters()).isNotNull();
	}
}
