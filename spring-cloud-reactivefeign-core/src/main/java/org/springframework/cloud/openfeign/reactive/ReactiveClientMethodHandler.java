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

import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.reactive.client.ReactiveClientFactory;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpClient;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static feign.Util.checkNotNull;
import static java.util.stream.Collectors.*;
import static org.springframework.cloud.openfeign.reactive.utils.FeignUtils.returnPublisherType;

/**
 * Method handler for asynchronous HTTP requests via {@link WebClient}.
 *
 * @author Sergii Karpenko
 */
public class ReactiveClientMethodHandler implements ReactiveMethodHandler {

	private final Target target;
	private final MethodMetadata methodMetadata;
	private final ReactiveHttpClient<Object> reactiveClient;
	private final DefaultUriBuilderFactory defaultUriBuilderFactory;
	private final Map<String, List<Function<Map<String, ?>, String>>> headerExpanders;
	private final Type returnPublisherType;

	private ReactiveClientMethodHandler(Target target, MethodMetadata methodMetadata,
			ReactiveHttpClient reactiveClient) {
		this.target = checkNotNull(target, "target must be not null");
		this.methodMetadata = checkNotNull(methodMetadata,
				"methodMetadata must be not null");
		this.reactiveClient = checkNotNull(reactiveClient, "client must be not null");
		this.defaultUriBuilderFactory = new DefaultUriBuilderFactory(target.url());
		Stream<AbstractMap.SimpleImmutableEntry<String, String>> simpleImmutableEntryStream = methodMetadata
				.template().headers().entrySet().stream()
				.flatMap(e -> e.getValue().stream()
						.map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)));
		this.headerExpanders = simpleImmutableEntryStream.collect(groupingBy(
				entry -> entry.getKey(),
				mapping(entry -> buildExpandHeaderFunction(entry.getValue()), toList())));

		this.returnPublisherType = returnPublisherType(methodMetadata);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Publisher invoke(final Object[] argv) {

		final ReactiveHttpRequest request = buildRequest(argv);

		return reactiveClient.executeRequest(request, returnPublisherType);
	}

	protected ReactiveHttpRequest buildRequest(Object[] argv) {

		Map<String, ?> substitutionsMap = methodMetadata.indexToName().entrySet().stream()
				.flatMap(e -> e.getValue().stream()
						.map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)))
				.collect(Collectors.toMap(Map.Entry::getValue,
						entry -> argv[entry.getKey()]));

		URI uri = defaultUriBuilderFactory.uriString(methodMetadata.template().url())
				.queryParams(parameters(argv)).build(substitutionsMap);

		return new ReactiveHttpRequest(methodMetadata.template().method(), uri,
				headers(argv, substitutionsMap), body(argv));
	}

	protected MultiValueMap<String, String> parameters(Object[] argv) {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		methodMetadata.template().queries()
				.forEach((key, value) -> parameters.addAll(key, (List) value));

		if (methodMetadata.formParams() != null) {
			methodMetadata.formParams()
					.forEach(param -> parameters.add(param, "{" + param + "}"));
		}

		if (methodMetadata.queryMapIndex() != null) {
			((Map<String, String>) argv[methodMetadata.queryMapIndex()])
					.forEach((key, value) -> parameters.add(key, value));
		}
		return parameters;
	}

	protected MultiValueMap<String, String> headers(Object[] argv,
			Map<String, ?> substitutionsMap) {

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		methodMetadata.template().headers().keySet()
				.forEach(headerName -> headers.addAll(headerName,
						headerExpanders.get(headerName).stream()
								.map(expander -> expander.apply(substitutionsMap))
								.collect(toList())));

		if (methodMetadata.headerMapIndex() != null) {
			((Map<String, String>) argv[methodMetadata.headerMapIndex()])
					.forEach(headers::add);
		}

		return headers;
	}

	protected Publisher<Object> body(Object[] argv) {
		if (methodMetadata.bodyIndex() != null) {
			Object body = argv[methodMetadata.bodyIndex()];
			if (body instanceof Publisher) {
				return (Publisher<Object>) body;
			}
			else {
				return Mono.just(body);
			}
		}
		else {
			return Mono.empty();
		}
	}

	// TODO refactor to chunks instead of regexp for better performance
	private Function<Map<String, ?>, String> buildExpandHeaderFunction(
			final String headerPattern) {
		return substitutionsMap -> {
			String headerExpanded = headerPattern;
			for (Map.Entry<String, ?> entry : substitutionsMap.entrySet()) {
				Pattern substitutionPattern = Pattern.compile("{" + entry.getKey() + "}",
						Pattern.LITERAL);
				headerExpanded = substitutionPattern.matcher(headerExpanded)
						.replaceAll(entry.getValue().toString());
			}
			return headerExpanded;
		};
	}

	public static class Factory implements ReactiveMethodHandlerFactory {
		private final ReactiveClientFactory reactiveClientFactory;

		public Factory(final ReactiveClientFactory reactiveClientFactory) {
			this.reactiveClientFactory = checkNotNull(reactiveClientFactory,
					"client must not be null");
		}

		@Override
		public ReactiveClientMethodHandler create(Target target,
				final MethodMetadata metadata) {

			return new ReactiveClientMethodHandler(target, metadata,
					reactiveClientFactory.apply(metadata));
		}
	}
}
