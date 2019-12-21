package org.springframework.cloud.openfeign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.support.Decode404;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringDecoderDecode404Tests extends SpringDecoderTests {

	@Test
	public void testDecodes404() {
		final ResponseEntity<String> response = testClient(true).getNotFound();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getBody()).as("response body was not null").isNull();
	}

	@Configuration
	static class Decode404Configruation {

		@Bean
		@Primary
		public Decode404 feignDecode404() {
			return () -> true;
		}
	}
}
