/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.reactive;

import static org.springframework.cloud.openfeign.reactive.Logger.MessageSupplier.msg;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpRequest;
import org.springframework.cloud.openfeign.reactive.client.WebReactiveHttpClient;
import org.springframework.http.HttpHeaders;

/**
 * @author Sergii Karpenko
 */

public class Logger {

	private final org.slf4j.Logger logger = LoggerFactory
			.getLogger(WebReactiveHttpClient.class);

	public void logRequest(String feignMethodTag, ReactiveHttpRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}]--->{} {} HTTP/1.1", feignMethodTag, request.method(),
					request.uri());
		}

		if (logger.isTraceEnabled()) {
			logger.trace("[{}] REQUEST HEADERS\n{}", feignMethodTag,
					msg(() -> request.headers().entrySet().stream()
							.map(entry -> String.format("%s:%s", entry.getKey(),
									entry.getValue()))
							.collect(Collectors.joining("\n"))));

			request.body().subscribe(ReactiveUtils.onNext(
					body -> logger.trace("[{}] REQUEST BODY\n{}", feignMethodTag, body)));
		}
	}

	public void logResponseHeaders(String feignMethodTag, HttpHeaders httpHeaders,
			long elapsedTime) {
		if (logger.isTraceEnabled()) {
			logger.trace("[{}] RESPONSE HEADERS\n{}", feignMethodTag,
					msg(() -> httpHeaders.entrySet().stream()
							.map(entry -> String.format("%s:%s", entry.getKey(),
									entry.getValue()))
							.collect(Collectors.joining("\n"))));
		}
		if (logger.isDebugEnabled()) {
			logger.debug("[{}]<--- headers takes {} milliseconds", feignMethodTag,
					elapsedTime);
		}
	}

	public void logResponseBodyAndTime(String feignMethodTag, Object response,
			long elapsedTime) {
		if (logger.isTraceEnabled()) {
			logger.debug("[{}]<---{}", feignMethodTag, response);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("[{}]<--- takes {} milliseconds", feignMethodTag, elapsedTime);
		}
	}

	public static class MessageSupplier {
		private Supplier<?> supplier;

		public MessageSupplier(Supplier<?> supplier) {
			this.supplier = supplier;
		}

		@Override
		public String toString() {
			return supplier.get().toString();
		}

		public static MessageSupplier msg(Supplier<?> supplier) {
			return new MessageSupplier(supplier);
		}
	}

}
