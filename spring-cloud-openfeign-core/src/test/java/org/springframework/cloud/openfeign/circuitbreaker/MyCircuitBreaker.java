/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

/**
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 */
class MyCircuitBreaker implements CircuitBreaker {

	AtomicBoolean runWasCalled = new AtomicBoolean();

	@Override
	public <T> T run(Supplier<T> toRun) {
		try {
			this.runWasCalled.set(true);
			return toRun.get();
		}
		catch (Throwable throwable) {
			throw new NoFallbackAvailableException("No fallback available.", throwable);
		}
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		try {
			return run(toRun);
		}
		catch (Throwable throwable) {
			return fallback.apply(throwable);
		}
	}

	public void clear() {
		this.runWasCalled.set(false);
	}

}
