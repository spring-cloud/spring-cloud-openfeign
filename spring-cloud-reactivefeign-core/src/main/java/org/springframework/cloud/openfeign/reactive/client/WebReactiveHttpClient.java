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

import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static org.springframework.cloud.openfeign.reactive.utils.FeignUtils.methodTag;

/**
 * Uses {@link WebClient} to execute http requests
 * @author Sergii Karpenko
 */
public class WebReactiveHttpClient<T> implements ReactiveHttpClient<T> {

	private final WebClient webClient;
	private final ParameterizedTypeReference<Object> bodyActualType;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference<T> returnActualType;
	private final String methodTag;

    public WebReactiveHttpClient(MethodMetadata methodMetadata, WebClient webClient) {
		this.webClient = webClient;

		Type bodyType = methodMetadata.bodyType();
		bodyActualType = getBodyActualType(bodyType);

		final Type returnType = methodMetadata.returnType();
		returnPublisherType = ((ParameterizedType) returnType).getRawType();
		returnActualType = ParameterizedTypeReference.forType(
				resolveLastTypeParameter(returnType, (Class<?>) returnPublisherType));
		methodTag = methodTag(methodMetadata);
	}

	@Override
	public Mono<ReactiveHttpResponse<T>> executeRequest(ReactiveHttpRequest request) {
		HttpMethod method = ofNullable(HttpMethod.resolve(request.method()))
				.orElseThrow(() -> new IllegalArgumentException("Unknown http method:"+request.method()));
		return webClient.method(method)
				.uri(request.uri())
				.headers(httpHeaders -> setUpHeaders(request, httpHeaders))
				.body(provideRequestBody(request))
				.exchange()
				.map((ClientResponse response) -> new WebReactiveHttpResponse(methodTag, response));
	}

	private class WebReactiveHttpResponse implements ReactiveHttpResponse<T>{

		private final String methodTag;
		private final ClientResponse response;

		private WebReactiveHttpResponse(String methodTag, ClientResponse response) {
			this.methodTag = methodTag;
			this.response = response;
		}

		@Override
		public String methodTag() {
			return methodTag;
		}

		@Override
		public int status() {
			return response.statusCode().value();
		}

		@Override
		public Headers headers() {
			return new Headers() {
				@Override
				public Set<Map.Entry<String, List<String>>> entries() {
					return response.headers().asHttpHeaders().entrySet();
				}

				@Override
				public List<String> get(String headerName) {
					return response.headers().asHttpHeaders().get(headerName);
				}
			};
		}

		@Override
		public Publisher<T> body() {
			return extractResponseBody(response);
		}

		@Override
		public Mono<byte[]> bodyData() {
			return DataBufferUtils.join(response.body(BodyExtractors.toDataBuffers()))
					.map(dataBuffer -> {
						byte[] bytes = new byte[dataBuffer.readableByteCount()];
						dataBuffer.read(bytes);
						DataBufferUtils.release(dataBuffer);
						return bytes;
					})
					.defaultIfEmpty(new byte[0]);
		}
	}

	protected BodyInserter<?, ? super ClientHttpRequest> provideRequestBody(
			ReactiveHttpRequest request) {
		return bodyActualType != null
				? BodyInserters.fromPublisher(request.body(), bodyActualType)
				: BodyInserters.empty();
	}

	protected void setUpHeaders(ReactiveHttpRequest request, HttpHeaders httpHeaders) {
		request.headers().forEach(httpHeaders::put);
	}

	protected Publisher<T> extractResponseBody(ClientResponse response){
		if (returnPublisherType == Mono.class) {
			return response.bodyToMono(returnActualType);
		} else {
			return response.bodyToFlux(returnActualType);
		}
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
