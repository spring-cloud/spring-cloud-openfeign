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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.http.converter.HttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FeignHttpMessageConverters}.
 *
 * @author seonwoo_jung
 */
class FeignHttpMessageConvertersTests {

	@Test
	void shouldNotExposePartiallyInitializedConvertersToConcurrentCallers() throws Exception {
		CountDownLatch initStarted = new CountDownLatch(1);
		CountDownLatch allowInitToFinish = new CountDownLatch(1);

		// A customizer that blocks while the converter list is being built so that we can
		// observe what a second, concurrent caller sees mid-initialization.
		ClientHttpMessageConvertersCustomizer blockingCustomizer = builder -> {
			initStarted.countDown();
			try {
				allowInitToFinish.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		};

		@SuppressWarnings("unchecked")
		ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers = mock(ObjectProvider.class);
		when(customizers.orderedStream()).thenReturn(Stream.of(blockingCustomizer));
		@SuppressWarnings("unchecked")
		ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers = mock(ObjectProvider.class);
		when(cloudCustomizers.iterator()).thenReturn(Collections.emptyIterator());

		FeignHttpMessageConverters feignConverters = new FeignHttpMessageConverters(customizers, cloudCustomizers);

		AtomicReference<List<HttpMessageConverter<?>>> initializerResult = new AtomicReference<>();
		AtomicReference<List<HttpMessageConverter<?>>> readerResult = new AtomicReference<>();
		AtomicBoolean readerObservedEmptyList = new AtomicBoolean();

		Thread initializer = new Thread(() -> initializerResult.set(feignConverters.getConverters()),
				"converters-init");
		Thread reader = new Thread(() -> {
			List<HttpMessageConverter<?>> converters = feignConverters.getConverters();
			readerObservedEmptyList.set(converters.isEmpty());
			readerResult.set(converters);
		}, "converters-reader");

		initializer.start();
		assertThat(initStarted.await(5, TimeUnit.SECONDS)).isTrue();

		// The first caller is now stuck building the list. Start a concurrent caller and
		// wait until it has either blocked waiting for initialization to complete (fixed
		// behaviour) or already returned a value (buggy behaviour) before releasing.
		reader.start();
		waitUntilBlockedOrFinished(reader);
		allowInitToFinish.countDown();

		initializer.join(TimeUnit.SECONDS.toMillis(5));
		reader.join(TimeUnit.SECONDS.toMillis(5));

		assertThat(initializerResult.get()).isNotEmpty();
		assertThat(readerObservedEmptyList).as("concurrent caller observed an empty converter list").isFalse();
		assertThat(readerResult.get()).isSameAs(initializerResult.get());
	}

	private static void waitUntilBlockedOrFinished(Thread thread) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			Thread.State state = thread.getState();
			if (state == Thread.State.BLOCKED || state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING
					|| state == Thread.State.TERMINATED) {
				return;
			}
			Thread.yield();
		}
	}

}
