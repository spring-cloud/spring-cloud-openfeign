package org.springframework.cloud.openfeign.reactive.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MultiValueMapUtils {

	public static <K, V> void addAll(Map<K, List<V>> multiMap, K key, List<V> values){
		multiMap.compute(key, (key_, values_) -> {
			List<V> valuesMerged = values_ != null ? values_ : new ArrayList<>(values.size());
			valuesMerged.addAll(values);
			return valuesMerged;
		});
	}

	public static <K, V> void add(Map<K, List<V>> multiMap, K key, V value){
		multiMap.compute(key, (key_, values_) -> {
			List<V> valuesMerged = values_ != null ? values_ : new ArrayList<>(1);
			valuesMerged.add(value);
			return valuesMerged;
		});
	}
}
