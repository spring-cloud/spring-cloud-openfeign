/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign.test.autoconfigure;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

/**
 * An HTTP server for the tests that answers every request with the same JSON body and
 * records the last request.
 *
 * @author Sharang Gupta
 */
public final class StubServer implements AutoCloseable {

	private final HttpServer server;

	private volatile RecordedRequest lastRequest;

	private StubServer(String json) {
		try {
			this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		this.server.createContext("/", exchange -> {
			this.lastRequest = new RecordedRequest(exchange.getRequestURI().getPath(), exchange.getRequestHeaders());
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream responseBody = exchange.getResponseBody()) {
				responseBody.write(body);
			}
		});
		this.server.start();
	}

	public static StubServer respondingWith(String json) {
		return new StubServer(json);
	}

	public String url() {
		return "http://localhost:" + this.server.getAddress().getPort();
	}

	public RecordedRequest lastRequest() {
		return this.lastRequest;
	}

	@Override
	public void close() {
		this.server.stop(0);
	}

	/**
	 * The path and the headers of a request that the server received.
	 *
	 * @param path the path of the request
	 * @param headers the headers of the request
	 */
	public record RecordedRequest(String path, Headers headers) {

	}

}
