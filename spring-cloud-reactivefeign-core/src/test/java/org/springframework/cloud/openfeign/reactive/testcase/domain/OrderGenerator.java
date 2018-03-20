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

package org.springframework.cloud.openfeign.reactive.testcase.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generator of random ice cream orders.
 */
public class OrderGenerator {
	private static final int[] BALLS_NUMBER = { 1, 3, 5, 7 };
	private static final int[] MIXIN_NUMBER = { 1, 2, 3 };

	private static final Random random = new Random();

	public IceCreamOrder generate(int id) {
		final IceCreamOrder order = new IceCreamOrder(id);
		final int nbBalls = peekBallsNumber();
		final int nbMixins = peekMixinNumber();

		IntStream.rangeClosed(1, nbBalls).mapToObj(i -> this.peekFlavor())
				.forEach(order::addBall);

		IntStream.rangeClosed(1, nbMixins).mapToObj(i -> this.peekMixin())
				.forEach(order::addMixin);

		return order;
	}

	public Collection<IceCreamOrder> generateRange(int n) {
		Instant now = Instant.now();

		List<Instant> orderTimestamps = IntStream.range(0, n)
				.mapToObj(minutes -> now.minus(minutes, ChronoUnit.MINUTES))
				.collect(Collectors.toList());

		return IntStream.range(0, n)
				.mapToObj(
						i -> this.generate(i).withOrderTimestamp(orderTimestamps.get(i)))
				.collect(Collectors.toList());
	}

	private int peekBallsNumber() {
		return BALLS_NUMBER[random.nextInt(BALLS_NUMBER.length)];
	}

	private int peekMixinNumber() {
		return MIXIN_NUMBER[random.nextInt(MIXIN_NUMBER.length)];
	}

	private Flavor peekFlavor() {
		return Flavor.values()[random.nextInt(Flavor.values().length)];
	}

	private Mixin peekMixin() {
		return Mixin.values()[random.nextInt(Mixin.values().length)];
	}
}
