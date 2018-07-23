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

package org.springframework.cloud.openfeign.reactive.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.cloud.openfeign.reactive.ReactiveFeign;
import org.springframework.cloud.openfeign.reactive.ReactiveOptions;
import org.springframework.cloud.openfeign.reactive.UriBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.CollectionUtils.toMultiValueMap;

/**
 * WebClient based implementation
 *
 * @author Sergii Karpenko
 */
public class WebClientReactiveFeign {

	public static <T> ReactiveFeign.Builder<T> builder() {
		return builder(WebClient.create());
	}

	public static <T> ReactiveFeign.Builder<T> builder(ReactiveOptions options) {
		WebClient webClient;
		if (!options.isEmpty()) {
			ReactorClientHttpConnector connector = new ReactorClientHttpConnector(
					opts -> {
						if (options.getConnectTimeoutMillis() != null) {
							opts.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
									options.getConnectTimeoutMillis());
						}
						if (options.getReadTimeoutMillis() != null) {
							opts.afterNettyContextInit(ctx -> {
								ctx.addHandlerLast(new ReadTimeoutHandler(
										options.getReadTimeoutMillis(),
										TimeUnit.MILLISECONDS));

							});
						}
						if (options.isTryUseCompression() != null) {
							opts.compression(options.isTryUseCompression());
						}
					});

			webClient = WebClient.builder().clientConnector(connector).build();
		} else {
			webClient = WebClient.create();
		}
		return builder(webClient);
	}

	public static <T> ReactiveFeign.Builder<T> builder(WebClient webClient) {
		return new ReactiveFeign.Builder<>(
				WebClientReactiveFeign::uriBuilder,
				methodMetadata -> new WebReactiveHttpClient<>(methodMetadata, webClient)
		);
	}

	private static UriBuilder uriBuilder(String targetUrl){

		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(targetUrl);

		return new UriBuilder() {

			private org.springframework.web.util.UriBuilder uriBuilder;

			@Override
			public UriBuilder fragment(String fragment) {
				uriBuilder = uriBuilderFactory.uriString(fragment);
				return this;
			}

			@Override
			public UriBuilder queryParameters(Map<String, List<String>> parameters) {
				uriBuilder = uriBuilder.queryParams(toMultiValueMap(parameters));
				return this;
			}

			@Override
			public URI build(Map<String, ?> uriVariables) {
				return uriBuilder.build(uriVariables);
			}
		};
	}
}



