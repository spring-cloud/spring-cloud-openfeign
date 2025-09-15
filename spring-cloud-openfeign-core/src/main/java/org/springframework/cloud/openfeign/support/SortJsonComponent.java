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

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.node.ArrayNode;

import org.springframework.data.domain.Sort;

/**
 * This class provides support for serializing and deserializing for Spring {@link Sort}
 * object.
 *
 * @author Can Bezmen
 * @author Olga Maciaszek-Sharma
 * @author Gokalp Kuscu
 */
public class SortJsonComponent {

	public static class SortSerializer extends ValueSerializer<Sort> {

		@Override
		public void serialize(Sort value, JsonGenerator gen, SerializationContext serializers) {
			gen.writeStartArray();
			value.iterator().forEachRemaining(gen::writePOJO);
			gen.writeEndArray();
		}

		@Override
		public Class<Sort> handledType() {
			return Sort.class;
		}

	}

	public static class SortDeserializer extends ValueDeserializer<Sort> {

		@Override
		public Sort deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
			TreeNode treeNode = jsonParser.readValueAsTree();
			if (treeNode.isArray()) {
				ArrayNode arrayNode = (ArrayNode) treeNode;
				return toSort(arrayNode);
			}
			else if (treeNode.get("orders") != null && treeNode.get("orders").isArray()) {
				ArrayNode arrayNode = (ArrayNode) treeNode.get("orders");
				return toSort(arrayNode);
			}
			return null;
		}

		@Override
		public Class<Sort> handledType() {
			return Sort.class;
		}

		private static Sort toSort(ArrayNode arrayNode) {
			List<Sort.Order> orders = new ArrayList<>();
			for (JsonNode jsonNode : arrayNode) {
				Sort.Order order;
				// there is no way to construct without null handling
				if ((jsonNode.has("ignoreCase") && jsonNode.get("ignoreCase").isBoolean())
						&& jsonNode.has("nullHandling") && jsonNode.get("nullHandling").isString()) {

					boolean ignoreCase = jsonNode.get("ignoreCase").asBoolean();
					String nullHandlingValue = jsonNode.get("nullHandling").asString();

					order = new Sort.Order(Sort.Direction.valueOf(jsonNode.get("direction").asString()),
							jsonNode.get("property").asString(), ignoreCase,
							Sort.NullHandling.valueOf(nullHandlingValue));
				}
				else {
					// backward compatibility
					order = new Sort.Order(Sort.Direction.valueOf(jsonNode.get("direction").asString()),
							jsonNode.get("property").asString());
				}
				orders.add(order);
			}
			return Sort.by(orders);
		}

	}

}
