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

package org.springframework.cloud.openfeign.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamController;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApiBroken;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Bill;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Flavor;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Mixin;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Sergii Karpenko
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		IcecreamController.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class ReactiveHttpClientTest {

	private IcecreamServiceApi client;

	@Autowired
	private IcecreamController icecreamController;

	@LocalServerPort
	private int port;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private String targetUrl;

	@Before
	public void setUp() {
		targetUrl = "http://localhost:" + port;
		client = ReactiveFeign.<IcecreamServiceApi>builder()
				.target(IcecreamServiceApi.class, targetUrl);
	}

	@Test
	public void testSimpleGet_success() {

		List<Flavor> flavors = client.getAvailableFlavors().collectList().block();
		List<Mixin> mixins = client.getAvailableMixins().collectList().block();

		assertThat(flavors).hasSize(Flavor.values().length)
				.containsAll(Arrays.asList(Flavor.values()));
		assertThat(mixins).hasSize(Mixin.values().length)
				.containsAll(Arrays.asList(Mixin.values()));

	}

	@Test
	public void testFindOrder_success() {

		IceCreamOrder order = client.findOrder(1).block();
		Assertions.assertThat(order).isEqualToComparingFieldByFieldRecursively(
				icecreamController.findOrder(1).block());

		Optional<IceCreamOrder> orderOptional = client.findOrder(123).blockOptional();
		assertThat(!orderOptional.isPresent());
	}

	@Test
	public void testMakeOrder_success() {

		IceCreamOrder order = new OrderGenerator().generate(20);

		Bill bill = client.makeOrder(order).block();
		assertThat(bill).isEqualToComparingFieldByField(Bill.makeBill(order));
	}

	@Test
	public void testPayBill_success() {

		Bill bill = Bill.makeBill(new OrderGenerator().generate(30));

		client.payBill(bill).block();
	}
}
