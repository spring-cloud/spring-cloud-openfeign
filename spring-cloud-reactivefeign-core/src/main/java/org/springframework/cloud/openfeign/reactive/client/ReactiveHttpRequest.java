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

import org.reactivestreams.Publisher;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static feign.Util.checkNotNull;

/**
 * An immutable reactive request to an http server.
 * @author Sergii Karpenko
 */
public final class ReactiveHttpRequest {

	private final String method;
	private final URI uri;
	private final MultiValueMap<String, String> headers;
	private final Publisher<Object> body;

	/**
	 * No parameters can be null except {@code body}. All parameters must be effectively
	 * immutable, via safe copies, not mutating or otherwise.
	 */
	public ReactiveHttpRequest(String method, URI uri,
			MultiValueMap<String, String> headers, Publisher<Object> body) {
		this.method = checkNotNull(method, "method of %s", uri);
		this.uri = checkNotNull(uri, "url");
		this.headers = checkNotNull(headers, "headers of %s %s", method, uri);
		this.body = body; // nullable
	}

	/* Method to invoke on the server. */
	public String method() {
		return method;
	}

	/* Fully resolved URL including query. */
	public URI uri() {
		return uri;
	}

	/* Ordered list of headers that will be sent to the server. */
	public MultiValueMap<String, String> headers() {
		return headers;
	}

	/**
	 * If present, this is the replayable body to send to the server.
	 */
	public Publisher<Object> body() {
		return body;
	}

}