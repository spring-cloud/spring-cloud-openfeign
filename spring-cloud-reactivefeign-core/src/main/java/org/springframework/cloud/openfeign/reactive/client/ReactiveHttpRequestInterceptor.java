package org.springframework.cloud.openfeign.reactive.client;

import java.util.function.Function;

/**
 * Used to modify request before call. May be used to set auth headers to all requests.
 *
 * @author Sergii Karpenko
 *
 */
public interface ReactiveHttpRequestInterceptor
		extends Function<ReactiveHttpRequest, ReactiveHttpRequest> {
}
