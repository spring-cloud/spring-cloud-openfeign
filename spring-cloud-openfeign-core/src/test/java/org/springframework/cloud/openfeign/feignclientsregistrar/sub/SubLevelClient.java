package org.springframework.cloud.openfeign.feignclientsregistrar.sub;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author Michal Domagala
 */

@FeignClient("sub-level")
public interface SubLevelClient {
}
