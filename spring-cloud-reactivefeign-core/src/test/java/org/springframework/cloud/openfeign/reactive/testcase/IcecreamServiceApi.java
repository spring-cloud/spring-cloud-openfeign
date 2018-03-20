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

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.openfeign.reactive.testcase.domain.Bill;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Flavor;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Mixin;

/**
 * API of an iceream web service.
 *
 * @author Sergii Karpenko
 */
@Headers({ "Accept: application/json" })
public interface IcecreamServiceApi {

	RuntimeException RUNTIME_EXCEPTION = new RuntimeException("tests exception");

	@RequestLine("GET /icecream/flavors")
	Flux<Flavor> getAvailableFlavors();

	@RequestLine("GET /icecream/mixins")
	Flux<Mixin> getAvailableMixins();

	@RequestLine("POST /icecream/orders")
	@Headers("Content-Type: application/json")
	Mono<Bill> makeOrder(IceCreamOrder order);

	@RequestLine("GET /icecream/orders/{orderId}")
	Mono<IceCreamOrder> findOrder(@Param("orderId") int orderId);

	@RequestLine("POST /icecream/bills/pay")
	@Headers("Content-Type: application/json")
	Mono<Void> payBill(Bill bill);

	default Mono<IceCreamOrder> findFirstOrder() {
		return findOrder(1);
	}

	default Mono<IceCreamOrder> throwExceptionMono() {
		throw RUNTIME_EXCEPTION;
	}
	default Flux<IceCreamOrder> throwExceptionFlux() {
		throw RUNTIME_EXCEPTION;
	}
}
