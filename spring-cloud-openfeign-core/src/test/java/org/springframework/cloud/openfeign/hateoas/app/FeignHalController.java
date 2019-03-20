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

package org.springframework.cloud.openfeign.hateoas.app;

import java.util.Collections;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hector Espert
 */
@RestController
public class FeignHalController {

	@GetMapping("/entity")
	public EntityModel<MarsRover> getEntity() {
		MarsRover marsRover = new MarsRover();
		marsRover.setName("Sojourner");
		Link link = new Link("/entity", "self");
		return new EntityModel<>(marsRover, link);
	}

	@GetMapping("/collection")
	public CollectionModel<MarsRover> getCollection() {
		MarsRover marsRover = new MarsRover();
		marsRover.setName("Opportunity");
		Link link = new Link("/collection", "self");
		return new CollectionModel<>(Collections.singleton(marsRover), link);
	}

	@GetMapping("/paged")
	public CollectionModel<MarsRover> getPaged() {
		MarsRover marsRover = new MarsRover();
		marsRover.setName("Curiosity");
		Link link = new Link("/paged", "self");
		PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(1, 1, 1);
		return new PagedModel<>(Collections.singleton(marsRover), metadata, link);
	}

}
