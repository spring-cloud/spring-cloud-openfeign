package org.springframework.cloud.openfeign;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonatan Ivanov
 */
class MetricsConfigurationTest {
	@Test
	void shouldBeEnabledByDefault() {
		FeignClientProperties.MetricsConfiguration config = new FeignClientProperties.MetricsConfiguration();
		assertThat(config.getEnabled()).isTrue();
	}

	@Test
	void shouldBeDisabledWhenSet() {
		FeignClientProperties.MetricsConfiguration config = new FeignClientProperties.MetricsConfiguration();
		config.setEnabled(false);
		assertThat(config.getEnabled()).isFalse();
	}

	@Test
	void shouldHaveValidEqualsAndHashCode() {
		EqualsVerifier.simple()
			.forClass(FeignClientProperties.MetricsConfiguration.class)
			.verify();
	}
}
