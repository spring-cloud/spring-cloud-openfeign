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

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.hateoas.app.FeignHalApplication;
import org.springframework.cloud.openfeign.hateoas.app.FeignHalClient;
import org.springframework.cloud.openfeign.hateoas.app.MarsRover;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test HATEOAS support.
 *
 * @author Hector Espert
 */
@SpringBootTest(classes = FeignHalApplication.class, webEnvironment = RANDOM_PORT,
		value = "debug=true")
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeignHalTests {

	@Autowired
	private FeignHalClient feignHalClient;

	@Test
	public void testEntityModel() {
		EntityModel<MarsRover> entity = feignHalClient.entity();
		assertThat(entity).isNotNull();

		assertThat(entity.hasLinks()).isTrue();
		assertThat(entity.hasLink("self")).isTrue();

		assertThat(entity.getLink("self")).map(Link::getHref).contains("/entity");

		MarsRover marsRover = entity.getContent();
		assertThat(marsRover).isNotNull();
		assertThat(marsRover.getName()).isEqualTo("Sojourner");
	}

	@Test
	public void testCollectionModel() {
		CollectionModel<MarsRover> collectionModel = feignHalClient.collection();
		assertThat(collectionModel).isNotNull();
		assertThat(collectionModel).isNotEmpty();

		assertThat(collectionModel.hasLinks()).isTrue();
		assertThat(collectionModel.hasLink("self")).isTrue();

		assertThat(collectionModel.getLink("self")).map(Link::getHref)
				.contains("/collection");

		Collection<MarsRover> collection = collectionModel.getContent();
		assertThat(collection).isNotEmpty();

		MarsRover marsRover = collection.stream().findAny().orElse(null);
		assertThat(marsRover).isNotNull();
		assertThat(marsRover.getName()).isEqualTo("Opportunity");
	}

	@Test
	public void testPagedModel() {
		PagedModel<MarsRover> paged = feignHalClient.paged();
		assertThat(paged).isNotNull();
		assertThat(paged).isNotEmpty();

		assertThat(paged.hasLinks()).isTrue();
		assertThat(paged.hasLink("self")).isTrue();

		assertThat(paged.getLink("self")).map(Link::getHref).contains("/paged");

		Collection<MarsRover> collection = paged.getContent();
		assertThat(collection).isNotEmpty();

		MarsRover marsRover = collection.stream().findAny().orElse(null);
		assertThat(marsRover).isNotNull();
		assertThat(marsRover.getName()).isEqualTo("Curiosity");
	}

}
