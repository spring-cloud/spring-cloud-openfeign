package org.springframework.cloud.openfeign.feignclientsregistrar;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author Michal Domagala
 */

@FeignClient("top-level")
public interface TopLevelClient {
}
