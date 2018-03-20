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
import java.util.*;

/**
 * Give me some ice-cream! :p
 */
public class IceCreamOrder {
	private static Random random = new Random();

	private int id; // order id
	private Map<Flavor, Integer> balls; // how much balls of flavor
	private Set<Mixin> mixins; // and some mixins ...
	private Instant orderTimestamp; // and give it to me right now !

	IceCreamOrder() {
	}

	IceCreamOrder(int id) {
		this(id, Instant.now());
	}

	IceCreamOrder(int id, final Instant orderTimestamp) {
		this.id = id;
		this.balls = new HashMap<>();
		this.mixins = new HashSet<>();
		this.orderTimestamp = orderTimestamp;
	}

	IceCreamOrder addBall(final Flavor ballFlavor) {
		final Integer ballCount = balls.containsKey(ballFlavor)
				? balls.get(ballFlavor) + 1 : 1;
		balls.put(ballFlavor, ballCount);
		return this;
	}

	IceCreamOrder addMixin(final Mixin mixin) {
		mixins.add(mixin);
		return this;
	}

	IceCreamOrder withOrderTimestamp(final Instant orderTimestamp) {
		this.orderTimestamp = orderTimestamp;
		return this;
	}

	public int getId() {
		return id;
	}

	public Map<Flavor, Integer> getBalls() {
		return balls;
	}

	public Set<Mixin> getMixins() {
		return mixins;
	}

	public Instant getOrderTimestamp() {
		return orderTimestamp;
	}

	@Override
	public String toString() {
		return "IceCreamOrder{" + " id=" + id + ", balls=" + balls + ", mixins=" + mixins
				+ ", orderTimestamp=" + orderTimestamp + '}';
	}
}
