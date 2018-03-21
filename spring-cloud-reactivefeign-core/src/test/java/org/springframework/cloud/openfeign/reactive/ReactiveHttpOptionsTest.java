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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Test the new capability of Reactive Feign client to support both Feign Request.Options
 * (regression) and the new ReactiveOptions configuration.
 *
 * @author Sergii Karpenko
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		IcecreamController.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class ReactiveHttpOptionsTest {

	private WebClient webClient = WebClient.create();

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
	}

	@Test
	public void testCompression() {

		ReactiveOptions options = new ReactiveOptions.Builder()
				.setConnectTimeoutMillis(5000).setReadTimeoutMillis(5000)
				.setTryUseCompression(true).build();

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(webClient).options(options)
				.target(IcecreamServiceApi.class, targetUrl);

		testClient(client);
	}

	/**
	 * Test the provided client for the correct results
	 *
	 * @param client Feign client instance
	 */
	private void testClient(IcecreamServiceApi client) {
		client.getAvailableFlavors().collectList().block();
	}
}
