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

package org.springframework.cloud.openfeign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static feign.Util.checkNotNull;

/**
 * Used to control the fallback given its cause.
 *
 * Ex.
 *
 * <pre>
 * {@code
 * // This instance will be invoked if there are errors of any kind.
 * FallbackFactory<GitHub> fallbackFactory = cause -> (owner, repo) -> {
 *   if (cause instanceof FeignException && ((FeignException) cause).status() == 403) {
 *     return Collections.emptyList();
 *   } else {
 *     return Arrays.asList("yogi");
 *   }
 * };
 *
 * GitHub github = FeignCircuitBreaker.builder()
 *                             ...
 *                             .target(GitHub.class, "https://api.github.com", fallbackFactory);
 * }
 * </pre>
 *
 * @param <T> the feign interface type
 */
public interface FallbackFactory<T> {

	/**
	 * Returns an instance of the fallback appropriate for the given cause.
	 * @param cause cause of an exception.
	 * @return fallback
	 */
	T create(Throwable cause);

	final class Default<T> implements FallbackFactory<T> {

		final Log logger;

		final T constant;

		public Default(T constant) {
			this(constant, LogFactory.getLog(Default.class));
		}

		Default(T constant, Log logger) {
			this.constant = checkNotNull(constant, "fallback");
			this.logger = checkNotNull(logger, "logger");
		}

		@Override
		public T create(Throwable cause) {
			if (logger.isTraceEnabled()) {
				logger.trace("fallback due to: " + cause.getMessage(), cause);
			}
			return constant;
		}

		@Override
		public String toString() {
			return constant.toString();
		}

	}

}
