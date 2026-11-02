package com.dowglasmaia.financialtransaction.services.client.maiaBank;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@CircuitBreaker(name = "maiabank")
@FeignClient(
      name = "MaiaBankClient",
      url = "${api.client.maiabank.url}"
)
public interface MaiaBankClient {

    @PostMapping
    ResponseEntity<Void> receiversPix();

}
