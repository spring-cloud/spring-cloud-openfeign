package org.springframework.cloud.openfeign.reactive.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reactive response from an http server.
 * @author Sergii Karpenko
 */
public interface ReactiveHttpResponse<T> {

	String methodTag();

	int status();

	Headers headers();

	Publisher<T> body();

	/**
	 * used by error decoders
	 * @return error message data
	 */
	Mono<byte[]> bodyData();

	interface Headers {
		Set<Map.Entry<String, List<String>>> entries();

		List<String> get(String headerName);
	}
}
