package com.dowglasmaia.exactbank.integrations;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
      name = "MaiaBankClient",
      url = "${api.client.maiabank.url}"
)
public interface MaiaBankClient {

    @PostMapping
    ResponseEntity<Void> receiversPix();

}
