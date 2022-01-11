/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.circuitbreaker;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

/**
 * Asynchronous circuit breaker.
 *
 * @author John Niang
 */
class AsyncCircuitBreaker implements CircuitBreaker {

	final Duration timeout;

	final ExecutorService executorService;

	AsyncCircuitBreaker(Duration timeout) {
		this(timeout, Executors.newCachedThreadPool());
	}

	AsyncCircuitBreaker(Duration timeout, ExecutorService executorService) {
		this.timeout = timeout;
		this.executorService = executorService;
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		Future<T> future = executorService.submit(toRun::get);
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (Throwable t) {
			return fallback.apply(t);
		}
	}

}
