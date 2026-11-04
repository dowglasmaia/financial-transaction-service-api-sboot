package com.dowglasmaia.financialtransaction.transaction.infrastructure.client;

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
    /** The request is unchanged (an empty POST). The real MaiaBank answers 200 with a text body, so the answer is read as text and ignored. */
    ResponseEntity<String> receiversPix();

}
