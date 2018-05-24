package org.springframework.cloud.openfeign.reactive.client;

import java.util.function.Function;

/**
 * @author Sergii Karpenko Used to modify request before call. May be used to set auth
 * headers to all requests.
 */
public interface ReactiveHttpRequestInterceptor
		extends Function<ReactiveHttpRequest, ReactiveHttpRequest> {
}
