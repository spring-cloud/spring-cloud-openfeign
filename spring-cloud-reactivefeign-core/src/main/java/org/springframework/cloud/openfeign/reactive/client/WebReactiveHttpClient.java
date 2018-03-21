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

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.reactive.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import feign.MethodMetadata;
import feign.Response;
import feign.codec.ErrorDecoder;
import reactor.core.publisher.Mono;

/**
 * Uses {@link WebClient} to execute http requests
 * @author Sergii Karpenko
 */
public class WebReactiveHttpClient implements ReactiveHttpClient {

	private final WebClient webClient;
	private final String methodTag;
	private final MethodMetadata metadata;
	private final ErrorDecoder errorDecoder;
	private final boolean decode404;
	private final Logger logger;
	private final ParameterizedTypeReference<Object> bodyActualType;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference<?> returnActualType;

	public WebReactiveHttpClient(MethodMetadata methodMetadata, WebClient webClient,
			ErrorDecoder errorDecoder, boolean decode404) {
		this.webClient = webClient;
		this.metadata = methodMetadata;
		this.errorDecoder = errorDecoder;
		this.decode404 = decode404;
		this.logger = new org.springframework.cloud.openfeign.reactive.Logger();

		this.methodTag = methodMetadata.configKey().substring(0,
				methodMetadata.configKey().indexOf('('));

		Type bodyType = methodMetadata.bodyType();
		bodyActualType = getBodyActualType(bodyType);

		final Type returnType = methodMetadata.returnType();
		returnPublisherType = ((ParameterizedType) returnType).getRawType();
		returnActualType = ParameterizedTypeReference.forType(
				resolveLastTypeParameter(returnType, (Class<?>) returnPublisherType));
	}

	@Override
	public Publisher<Object> executeRequest(ReactiveHttpRequest request) {
		logger.logRequest(methodTag, request);

		long start = System.currentTimeMillis();
		WebClient.ResponseSpec response = webClient.method(request.method())
				.uri(request.uri())
				.headers(httpHeaders -> setUpHeaders(request, httpHeaders))
				.body(provideBody(request))
				.retrieve()
				.onStatus(httpStatus -> decode404 && httpStatus == NOT_FOUND,
						  clientResponse -> null)
				.onStatus(HttpStatus::isError, this::decodeError)
				.onStatus(httpStatus -> true, logResponseHeaders(start));

		if (returnPublisherType == Mono.class) {
			return response.bodyToMono(returnActualType).map(responseLogger(start));
		}
		else {
			return response.bodyToFlux(returnActualType).map(responseLogger(start));
		}
	}

	protected Function<ClientResponse, Mono<? extends Throwable>> logResponseHeaders(long start) {
		return clientResponse -> {
			logger.logResponseHeaders(methodTag,
					clientResponse.headers().asHttpHeaders(),
					System.currentTimeMillis() - start);
			return null;
		};
	}

	private <V> Function<V, V> responseLogger(long start) {
		return result -> {
            logger.logResponseBodyAndTime(methodTag, result, System.currentTimeMillis() - start);
            return result;
        };
	}

	protected BodyInserter<?, ? super ClientHttpRequest> provideBody(ReactiveHttpRequest request) {
		return bodyActualType != null
                ? BodyInserters.fromPublisher(request.body(), bodyActualType)
                : BodyInserters.empty();
	}

	protected Mono<Exception> decodeError(ClientResponse clientResponse) {
		return clientResponse
        .bodyToMono(ByteArrayResource.class)
        .map(ByteArrayResource::getByteArray).defaultIfEmpty(new byte[0])
        .map(bodyData -> errorDecoder.decode(metadata.configKey(),
				Response.builder()
						.status(clientResponse.statusCode().value())
						.reason(clientResponse.statusCode()
								.getReasonPhrase())
						.headers(clientResponse.headers().asHttpHeaders()
								.entrySet().stream()
								.collect(Collectors.toMap(
										Map.Entry::getKey,
										Map.Entry::getValue)))
						.body(bodyData).build()));
	}

	protected void setUpHeaders(ReactiveHttpRequest request, HttpHeaders httpHeaders) {
		request.headers().forEach(httpHeaders::put);
	}

	private ParameterizedTypeReference<Object> getBodyActualType(Type bodyType) {
		return ofNullable(bodyType).map(type -> {
			if (type instanceof ParameterizedType) {
				Class<?> returnBodyType = (Class<?>) ((ParameterizedType) type)
						.getRawType();
				if ((returnBodyType).isAssignableFrom(Publisher.class)) {
					return ParameterizedTypeReference
							.forType(resolveLastTypeParameter(bodyType, returnBodyType));
				}
				else {
					return ParameterizedTypeReference.forType(type);
				}
			}
			else {
				return ParameterizedTypeReference.forType(type);
			}
		}).orElse(null);
	}
}
