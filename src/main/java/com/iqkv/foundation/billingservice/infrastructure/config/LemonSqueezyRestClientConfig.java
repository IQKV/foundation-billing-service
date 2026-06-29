/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.billingservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnGateway(com.iqkv.foundation.billingservice.gateway.GatewayType.LEMON_SQUEEZY)
public class LemonSqueezyRestClientConfig {

  private static final String BASE_URL = "https://api.lemonsqueezy.com/v1";

  @Bean("lemonSqueezyRestClient")
  public RestClient lemonSqueezyRestClient(final LemonSqueezyConfigurationProperties props) {
    return RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.api+json")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.api+json")
        .build();
  }
}
