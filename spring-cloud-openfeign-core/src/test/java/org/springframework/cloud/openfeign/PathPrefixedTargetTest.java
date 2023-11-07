/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.openfeign;

import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PathPrefixedTargetTest {
	private final String url = "http://localhost:8080";
	private final Target<?> target;

	PathPrefixedTargetTest(@Mock Target<?> target) {
		this.target = target;
	}

	@BeforeEach
	void setUp() {
		when(this.target.url()).thenReturn(this.url);
	}

	@Test
	void urlAndPathAreConcatenated() {
		String path = "/common";
		PathPrefixedTarget<?> pathPrefixedTarget = new PathPrefixedTarget<>(path, this.target);

		assertThat(pathPrefixedTarget.url()).isEqualTo(this.url + path);
	}

	@Test
	void concatenatedUrlIsPassedIntoRequestTemplate(@Mock RequestTemplate template) {
		String path = "/common";
		PathPrefixedTarget<?> pathPrefixedTarget = new PathPrefixedTarget<>(path, this.target);

		pathPrefixedTarget.apply(template);

		verify(template).target(pathPrefixedTarget.url());
	}
}
