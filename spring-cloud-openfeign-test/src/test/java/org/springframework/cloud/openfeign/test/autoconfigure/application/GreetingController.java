/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.test.autoconfigure.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller that uses the {@link GreetingClient}.
 *
 * @author Sharang Gupta
 */
@RestController
class GreetingController {

	private final GreetingClient greetingClient;

	GreetingController(GreetingClient greetingClient) {
		this.greetingClient = greetingClient;
	}

	@GetMapping("/hello/{name}")
	String hello(@PathVariable("name") String name) {
		return this.greetingClient.greet(name).message();
	}

}
