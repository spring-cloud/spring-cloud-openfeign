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

package org.springframework.cloud.openfeign.reactive.client;

import static org.springframework.cloud.openfeign.reactive.ReactiveUtils.onNext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;

import feign.MethodMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Wraps {@link ReactiveHttpClient} with retry logic provided by retryFunction
 * @author Sergii Karpenko
 */
public class RetryReactiveHttpClient implements ReactiveHttpClient {

	private static final org.slf4j.Logger logger = LoggerFactory
			.getLogger(RetryReactiveHttpClient.class);

	private final ReactiveHttpClient reactiveClient;
	private final Function<Flux<Throwable>, Publisher<?>> retryFunction;
	private final Type returnPublisherType;

	public RetryReactiveHttpClient(ReactiveHttpClient reactiveClient,
			MethodMetadata methodMetadata,
			Function<Flux<Throwable>, Publisher<Throwable>> retryFunction) {
		this.reactiveClient = reactiveClient;
		String methodTag = methodMetadata.configKey().substring(0,
				methodMetadata.configKey().indexOf('('));
		this.retryFunction = wrapWithLog(retryFunction, methodTag);
		final Type returnType = methodMetadata.returnType();
		returnPublisherType = ((ParameterizedType) returnType).getRawType();
	}

	@Override
	public Publisher<Object> executeRequest(ReactiveHttpRequest request) {
		Publisher<Object> objectPublisher = reactiveClient.executeRequest(request);
		if (returnPublisherType == Mono.class) {
			return ((Mono<Object>) objectPublisher).retryWhen(retryFunction);
		}
		else {
			return ((Flux<Object>) objectPublisher).retryWhen(retryFunction);
		}
	}

	private static Function<Flux<Throwable>, Publisher<?>> wrapWithLog(
			Function<Flux<Throwable>, Publisher<Throwable>> retryFunction,
			String feignMethodTag) {
		return throwableFlux -> {
			Publisher<Throwable> publisher = retryFunction.apply(throwableFlux);
			publisher.subscribe(onNext(throwable -> {
				if (logger.isDebugEnabled()) {
					logger.debug("[{}]---> RETRYING on error", feignMethodTag, throwable);
				}
			}));
			return publisher;
		};
	}
}
