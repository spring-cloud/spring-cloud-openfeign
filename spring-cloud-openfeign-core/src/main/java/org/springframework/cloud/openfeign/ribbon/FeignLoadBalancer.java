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

package org.springframework.cloud.openfeign.ribbon;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Client;
import feign.Request;
import feign.Response;

import org.springframework.cloud.netflix.ribbon.RibbonProperties;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToSecureConnectionIfNeeded;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Ryan Baxter
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 */
public class FeignLoadBalancer extends
		AbstractLoadBalancerAwareClient<FeignLoadBalancer.RibbonRequest, FeignLoadBalancer.RibbonResponse> {

	private final RibbonProperties ribbon;

	protected int connectTimeout;

	protected int readTimeout;

	protected IClientConfig clientConfig;

	protected ServerIntrospector serverIntrospector;

	protected boolean followRedirects;

	public FeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig,
			ServerIntrospector serverIntrospector) {
		super(lb, clientConfig);
		setRetryHandler(RetryHandler.DEFAULT);
		this.clientConfig = clientConfig;
		this.ribbon = RibbonProperties.from(clientConfig);
		RibbonProperties ribbon = this.ribbon;
		connectTimeout = ribbon.getConnectTimeout();
		readTimeout = ribbon.getReadTimeout();
		this.serverIntrospector = serverIntrospector;
		followRedirects = ribbon.isFollowRedirects();
	}

	@Override
	public RibbonResponse execute(RibbonRequest request, IClientConfig configOverride)
			throws IOException {
		Request.Options options;
		if (configOverride != null) {
			RibbonProperties override = RibbonProperties.from(configOverride);
			options = new Request.Options(override.connectTimeout(connectTimeout),
					TimeUnit.MILLISECONDS, override.readTimeout(readTimeout),
					TimeUnit.MILLISECONDS, override.isFollowRedirects(followRedirects));
		}
		else {
			options = new Request.Options(connectTimeout, TimeUnit.MILLISECONDS,
					readTimeout, TimeUnit.MILLISECONDS, followRedirects);
		}
		Response response = request.client().execute(request.toRequest(), options);
		return new RibbonResponse(request.getUri(), response);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			RibbonRequest request, IClientConfig requestConfig) {
		if (ribbon.isOkToRetryOnAllOperations()) {
			return new RequestSpecificRetryHandler(true, true, getRetryHandler(),
					requestConfig);
		}
		if (!request.toRequest().httpMethod().name().equals("GET")) {
			return new RequestSpecificRetryHandler(true, false, getRetryHandler(),
					requestConfig);
		}
		else {
			return new RequestSpecificRetryHandler(true, true, getRetryHandler(),
					requestConfig);
		}
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		URI uri = updateToSecureConnectionIfNeeded(original, clientConfig,
				serverIntrospector, server);
		return super.reconstructURIWithServer(server, uri);
	}

	protected static class RibbonRequest extends ClientRequest implements Cloneable {

		private final Request request;

		private final Client client;

		protected RibbonRequest(Client client, Request request, URI uri) {
			this.client = client;
			setUri(uri);
			this.request = toRequest(request);
		}

		private Request toRequest(Request request) {
			Map<String, Collection<String>> headers = new LinkedHashMap<>(
					request.headers());
			return Request.create(request.httpMethod(), getUri().toASCIIString(), headers,
					request.body(), request.charset(), request.requestTemplate());
		}

		Request toRequest() {
			return toRequest(request);
		}

		Client client() {
			return client;
		}

		HttpRequest toHttpRequest() {
			return new HttpRequest() {
				@Override
				public HttpMethod getMethod() {
					return HttpMethod
							.resolve(RibbonRequest.this.toRequest().httpMethod().name());
				}

				@Override
				public String getMethodValue() {
					return getMethod().name();
				}

				@Override
				public URI getURI() {
					return RibbonRequest.this.getUri();
				}

				@Override
				public HttpHeaders getHeaders() {
					Map<String, List<String>> headers = new HashMap<>();
					Map<String, Collection<String>> feignHeaders = RibbonRequest.this
							.toRequest().headers();
					for (String key : feignHeaders.keySet()) {
						headers.put(key, new ArrayList<>(feignHeaders.get(key)));
					}
					HttpHeaders httpHeaders = new HttpHeaders();
					httpHeaders.putAll(headers);
					return httpHeaders;

				}
			};
		}

		public Request getRequest() {
			return request;
		}

		public Client getClient() {
			return client;
		}

		@Override
		public Object clone() {
			return new RibbonRequest(client, request, getUri());
		}

	}

	protected static class RibbonResponse implements IResponse {

		private final URI uri;

		private final Response response;

		protected RibbonResponse(URI uri, Response response) {
			this.uri = uri;
			this.response = response;
		}

		@Override
		public Object getPayload() {
			return response.body();
		}

		@Override
		public boolean hasPayload() {
			return response.body() != null;
		}

		@Override
		public boolean isSuccess() {
			return response.status() == 200;
		}

		@Override
		public URI getRequestedURI() {
			return uri;
		}

		@Override
		public Map<String, Collection<String>> getHeaders() {
			return response.headers();
		}

		Response toResponse() {
			return response;
		}

		@Override
		public void close() throws IOException {
			if (response != null && response.body() != null) {
				response.body().close();
			}
		}

	}

}
