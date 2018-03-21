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

package org.springframework.cloud.openfeign.reactive.testcase;

import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.openfeign.reactive.testcase.domain.Bill;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Flavor;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Mixin;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller of an iceream web service.
 *
 * @author Sergii Karpenko
 */
@RestController
public class IcecreamController implements IcecreamServiceApi {

	private OrderGenerator generator = new OrderGenerator();
	private Map<Integer, IceCreamOrder> orders;

	IcecreamController() {
		orders = generator.generateRange(10).stream()
				.collect(Collectors.toMap(IceCreamOrder::getId, o -> o));
	}

	@GetMapping(path = "/icecream/flavors")
	@Override
	public Flux<Flavor> getAvailableFlavors() {
		return Flux.fromArray(Flavor.values());
	}

	@GetMapping(path = "/icecream/mixins")
	@Override
	public Flux<Mixin> getAvailableMixins() {
		return Flux.fromArray(Mixin.values());
	}

	@GetMapping(path = "/icecream/orders/{orderId}")
	@Override
	public Mono<IceCreamOrder> findOrder(@PathVariable("orderId") int orderId) {
		IceCreamOrder order = orders.get(orderId);
		return order != null ? just(order) : empty();
	}

	@PostMapping(path = "/icecream/orders")
	@Override
	public Mono<Bill> makeOrder(@RequestBody IceCreamOrder order) {
		return just(Bill.makeBill(order));
	}

	@PostMapping(path = "/icecream/bills/pay")
	@Override
	public Mono<Void> payBill(@RequestBody Bill bill) {
		return empty();
	}
}
