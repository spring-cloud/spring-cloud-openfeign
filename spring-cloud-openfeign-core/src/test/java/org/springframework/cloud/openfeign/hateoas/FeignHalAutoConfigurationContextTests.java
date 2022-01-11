/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.hateoas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.config.WebConverters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Hector Espert
 * @author Olga Maciaszek-Sharma
 */
class FeignHalAutoConfigurationContextTests {

	private WebApplicationContextRunner contextRunner;

	@BeforeEach
	void setUp() {
		contextRunner = new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
						HttpMessageConvertersAutoConfiguration.class, HypermediaAutoConfiguration.class,
						RepositoryRestMvcAutoConfiguration.class, FeignHalAutoConfiguration.class))
				.withPropertyValues("debug=true");
	}

	@Test
	void shouldNotLoadWebConvertersCustomizerWhenNotWebConvertersNotInClasspath() {
		FilteredClassLoader filteredClassLoader = new FilteredClassLoader(RepositoryRestMvcConfiguration.class,
				WebConverters.class);
		contextRunner.withClassLoader(filteredClassLoader)
				.run(context -> assertThat(context).doesNotHaveBean("webConvertersCustomizer"));
	}

	@Test
	void shouldLoadWebConvertersCustomizer() {
		FilteredClassLoader filteredClassLoader = new FilteredClassLoader(RepositoryRestMvcConfiguration.class);
		contextRunner.withClassLoader(filteredClassLoader)
				.run(context -> assertThat(context).hasBean("webConvertersCustomizer"));
	}

}
