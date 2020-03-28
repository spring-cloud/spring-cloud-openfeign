package org.springframework.cloud.openfeign.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PojoJsonSerializationWriter extends PojoSerializationWriter {
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	protected MediaType getContentType() {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	protected String serialize(Object object) throws IOException {
		return objectMapper.writeValueAsString(object);
	}
}
