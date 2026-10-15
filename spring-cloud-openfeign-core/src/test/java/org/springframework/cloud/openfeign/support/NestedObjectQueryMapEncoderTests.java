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

package org.springframework.cloud.openfeign.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import feign.Param;
import feign.QueryMapEncoder;
import feign.codec.EncodeException;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link NestedObjectQueryMapEncoder}.
 *
 * @author Sharang Gupta
 */
class NestedObjectQueryMapEncoderTests {

	private final QueryMapEncoder encoder = new NestedObjectQueryMapEncoder(new SpringFieldQueryMapEncoder());

	@Test
	void flatObjectIsEncodedAsBefore() {
		Search search = new Search();
		search.query = "shoes";
		search.page = 2;

		assertThat(encoder.encode(search)).containsOnly(Map.entry("query", "shoes"), Map.entry("page", 2));
	}

	@Test
	void nestedObjectIsEncodedWithThePropertyPath() {
		Search search = new Search();
		search.price = new Range(10, 20);

		assertThat(encoder.encode(search)).containsOnly(Map.entry("price.from", 10), Map.entry("price.to", 20));
	}

	@Test
	void deeplyNestedObjectsAreEncodedWithTheFullPropertyPath() {
		Search search = new Search();
		search.price = new Range(10, 20);
		search.price.currency = new Currency("EUR");

		assertThat(encoder.encode(search)).containsEntry("price.currency.code", "EUR");
	}

	@Test
	void objectsOfACollectionAreEncodedWithTheirIndex() {
		Search search = new Search();
		search.filters = List.of(new Filter("color", "red"), new Filter("size", "42"));

		assertThat(encoder.encode(search)).containsOnly(Map.entry("filters[0].name", "color"),
				Map.entry("filters[0].value", "red"), Map.entry("filters[1].name", "size"),
				Map.entry("filters[1].value", "42"));
	}

	@Test
	void objectsOfAnArrayAreEncodedWithTheirIndex() {
		Search search = new Search();
		search.ranges = new Range[] { new Range(1, 2), new Range(3, 4) };

		assertThat(encoder.encode(search)).containsOnly(Map.entry("ranges[0].from", 1), Map.entry("ranges[0].to", 2),
				Map.entry("ranges[1].from", 3), Map.entry("ranges[1].to", 4));
	}

	@Test
	void collectionOfSimpleValuesIsKeptAsRepeatedParameters() {
		Search search = new Search();
		search.tags = List.of("sale", "new");

		assertThat(encoder.encode(search)).containsOnly(Map.entry("tags", List.of("sale", "new")));
	}

	@Test
	void arrayOfSimpleValuesIsSentAsRepeatedParameters() {
		Search search = new Search();
		search.ids = new int[] { 1, 2 };

		assertThat(encoder.encode(search)).containsOnly(Map.entry("ids", List.of(1, 2)));
	}

	@Test
	void entriesOfAMapAreEncodedWithTheirKey() {
		Search search = new Search();
		search.attributes = new LinkedHashMap<>();
		search.attributes.put("color", "red");
		search.attributes.put("price", new Range(10, 20));

		assertThat(encoder.encode(search)).containsOnly(Map.entry("attributes[color]", "red"),
				Map.entry("attributes[price].from", 10), Map.entry("attributes[price].to", 20));
	}

	@Test
	void simpleValueTypesAreNotNested() {
		Search search = new Search();
		search.id = UUID.fromString("0f9a8a9e-6d8c-4c0a-9f8e-4b3c2a1d0e9f");
		search.since = LocalDate.of(2026, 10, 13);
		search.amount = new BigDecimal("9.99");
		search.status = Status.OPEN;

		assertThat(encoder.encode(search)).containsOnly(Map.entry("id", search.id), Map.entry("since", search.since),
				Map.entry("amount", search.amount), Map.entry("status", Status.OPEN));
	}

	@Test
	void nullNestedObjectIsSkipped() {
		Search search = new Search();
		search.query = "shoes";
		search.price = null;

		assertThat(encoder.encode(search)).containsOnly(Map.entry("query", "shoes"));
	}

	@Test
	void nullObjectIsEncodedAsAnEmptyMap() {
		assertThat(encoder.encode(null)).isEmpty();
	}

	@Test
	void parameterAliasOfANestedFieldIsHonored() {
		Search search = new Search();
		search.price = new Range(10, 20);
		search.price.unit = "cents";

		assertThat(encoder.encode(search)).containsEntry("price.currency_unit", "cents");
	}

	@Test
	void objectNestedInItselfIsRejected() {
		Node parent = new Node();
		Node child = new Node();
		parent.child = child;
		child.parent = parent;

		assertThatExceptionOfType(EncodeException.class).isThrownBy(() -> encoder.encode(parent))
			.withMessageContaining("nested in itself")
			.withMessageContaining("child.parent");
	}

	@Test
	void mapNestedInItselfIsRejected() {
		Search search = new Search();
		search.attributes = new LinkedHashMap<>();
		search.attributes.put("self", search.attributes);

		assertThatExceptionOfType(EncodeException.class).isThrownBy(() -> encoder.encode(search))
			.withMessageContaining("nested in itself")
			.withMessageContaining("attributes[self]");
	}

	@Test
	void collectionNestedInItselfIsRejected() {
		Holder holder = new Holder();
		holder.items = new ArrayList<>();
		holder.items.add(holder.items);

		assertThatExceptionOfType(EncodeException.class).isThrownBy(() -> encoder.encode(holder))
			.withMessageContaining("nested in itself")
			.withMessageContaining("items[0]");
	}

	@Test
	void arrayNestedInItselfIsRejected() {
		Holder holder = new Holder();
		holder.elements = new Object[1];
		holder.elements[0] = holder.elements;

		assertThatExceptionOfType(EncodeException.class).isThrownBy(() -> encoder.encode(holder))
			.withMessageContaining("nested in itself")
			.withMessageContaining("elements[0]");
	}

	@Test
	void cycleBelowTheQueryMapObjectIsRejected() {
		List<Object> items = new ArrayList<>();
		Search search = new Search();
		search.attributes = new LinkedHashMap<>();
		search.attributes.put("items", items);
		items.add(search.attributes);

		assertThatExceptionOfType(EncodeException.class).isThrownBy(() -> encoder.encode(search))
			.withMessageContaining("nested in itself")
			.withMessageContaining("attributes[items][0]");
	}

	@Test
	void mapReferencedTwiceIsEncodedTwice() {
		Map<String, Object> shared = Map.of("color", "red");
		Search search = new Search();
		search.attributes = new LinkedHashMap<>();
		search.attributes.put("front", shared);
		search.attributes.put("back", shared);

		assertThat(encoder.encode(search)).containsOnly(Map.entry("attributes[front][color]", "red"),
				Map.entry("attributes[back][color]", "red"));
	}

	@Test
	void worksWithThePageableQueryMapEncoder() {
		QueryMapEncoder pageableEncoder = new NestedObjectQueryMapEncoder(new PageableSpringQueryMapEncoder());

		assertThat(pageableEncoder.encode(PageRequest.of(1, 10))).containsOnly(Map.entry("page", 1),
				Map.entry("size", 10));
	}

	enum Status {

		OPEN, CLOSED

	}

	static class Search {

		String query;

		Integer page;

		UUID id;

		LocalDate since;

		BigDecimal amount;

		Status status;

		Range price;

		Range[] ranges;

		List<Filter> filters;

		List<String> tags;

		int[] ids;

		Map<String, Object> attributes;

	}

	static class Range {

		Integer from;

		Integer to;

		Currency currency;

		@Param("currency_unit")
		String unit;

		Range(Integer from, Integer to) {
			this.from = from;
			this.to = to;
		}

	}

	static class Currency {

		String code;

		Currency(String code) {
			this.code = code;
		}

	}

	static class Filter {

		String name;

		String value;

		Filter(String name, String value) {
			this.name = name;
			this.value = value;
		}

	}

	static class Holder {

		List<Object> items;

		Object[] elements;

	}

	static class Node {

		Node parent;

		Node child;

	}

}
