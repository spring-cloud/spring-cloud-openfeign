package org.springframework.cloud.openfeign.reactive;

import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.function.Function;

public interface ReactiveRetryPolicy {
	/**
	 * @param error
	 * @param attemptNo
	 * @return -1 if should not be retried, 0 if retry immediately
	 */
	long retryDelay(Throwable error, int attemptNo);

	default Function<Flux<Throwable>, Publisher<Throwable>> toRetryFunction() {
		return errors -> errors
				.zipWith(Flux.range(1, Integer.MAX_VALUE), (error, index) -> {
					long delay = retryDelay(error, index);
					if (delay >= 0) {
						return Tuples.of(delay, error);
					}
					else {
						throw Exceptions.propagate(error);
					}
				}).flatMap(
						tuple2 -> tuple2.getT1() > 0
								? Mono.delay(Duration.ofMillis(tuple2.getT1()))
										.map(time -> tuple2.getT2())
								: Mono.just(tuple2.getT2()));
	}
}
