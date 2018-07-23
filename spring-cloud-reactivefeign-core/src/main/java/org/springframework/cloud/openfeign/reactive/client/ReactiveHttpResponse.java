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

	int status();

	Map<String, List<String>> headers();

	Publisher<T> body();

	/**
	 * used by error decoders
	 * @return error message data
	 */
	Mono<byte[]> bodyData();
}
