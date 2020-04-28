package org.springframework.cloud.openfeign.support;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author canbezmen
 */
class SortJacksonModuleTests {

	private static ObjectMapper objectMapper;

	@BeforeAll
	public static void initialize() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModules(new PageJacksonModule());
		objectMapper.registerModule(new SortJacksonModule());
	}

	@Test
	public void deserializePage() throws JsonProcessingException {
		// Given
		String pageJson = "{\"content\":[\"A name\"],\"number\":1,\"size\":2,\"totalElements\":3,\"sort\":[{\"direction\":\"ASC\",\"property\":\"field\",\"ignoreCase\":false,\"nullHandling\":\"NATIVE\",\"descending\":false,\"ascending\":true}]}";
		// When
		Page<?> result = objectMapper.readValue(pageJson, Page.class);
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getPageable()).isNotNull();
		assertThat(result.getPageable().getPageSize()).isEqualTo(2);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(1);
		assertThat(result.getPageable().getSort()).hasSize(1);
		Optional<Sort.Order> optionalOrder = result.getPageable().getSort().get()
			.findFirst();
		if (optionalOrder.isPresent()) {
			Sort.Order order = optionalOrder.get();
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
			assertThat(order.getProperty()).isEqualTo("field");
		}
	}

}
