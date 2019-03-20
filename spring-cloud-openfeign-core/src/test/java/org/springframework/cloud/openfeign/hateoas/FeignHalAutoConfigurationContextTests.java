/*
 * Copyright 2016-2019 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.RepresentationModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Hector Espert
 */
public class FeignHalAutoConfigurationContextTests {

	private WebApplicationContextRunner contextRunner;

	@Before
	public void setUp() {
		contextRunner = new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
						HttpMessageConvertersAutoConfiguration.class,
						HypermediaAutoConfiguration.class,
						RepositoryRestMvcAutoConfiguration.class,
						FeignHalAutoConfiguration.class))
				.withPropertyValues("debug=true");
	}

	@Test
	public void testHalJacksonHttpMessageConverterIsNotLoaded() {
		FilteredClassLoader filteredClassLoader = new FilteredClassLoader(
				RepositoryRestMvcConfiguration.class, RepresentationModel.class);
		contextRunner.withClassLoader(filteredClassLoader)
				.run(context -> assertThat(context)
						.doesNotHaveBean("halJacksonHttpMessageConverter"));
	}

	@Test
	public void testHalJacksonHttpMessageConverterIsLoaded() {
		FilteredClassLoader filteredClassLoader = new FilteredClassLoader(
				RepositoryRestMvcConfiguration.class);
		contextRunner.withClassLoader(filteredClassLoader).run(
				context -> assertThat(context).hasBean("halJacksonHttpMessageConverter"));
	}

	@Test
	public void testHalJacksonHttpMessageConverterIsNotLoadedUseRestDataMessageConverterInstead() {
		contextRunner.run(
				context -> assertThat(context).hasBean("halJacksonHttpMessageConverter"));
	}

}
