/*
 * Copyright 2026 iQKV Foundation Team.
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

package com.iqkv.foundation.billingservice.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionRestResource Authorization Tests")
class SubscriptionRestResourceTest {

  private static final String TENANT_KEY = "tenant12";

  @Mock
  private SubscriptionService subscriptionService;

  @Mock
  private SubscriptionSubjectResolver subjectResolver;

  @Mock
  private Jwt jwt;

  private final UUID userId = UUID.randomUUID();
  private SubscriptionRestResource resource;

  private final SubscriptionDtos.CreateCheckoutSessionRequest request =
      new SubscriptionDtos.CreateCheckoutSessionRequest("plan-pro", "https://ok", "https://cancel", null, null, null);

  @BeforeEach
  void setUp() {
    resource = new SubscriptionRestResource(subscriptionService, subjectResolver);
    when(jwt.getClaimAsString("tenant_id")).thenReturn(TENANT_KEY);
    when(jwt.getClaimAsString("user_id")).thenReturn(userId.toString());
  }

  @Test
  @DisplayName("Should deny tenant-scoped checkout for a MEMBER")
  void shouldDenyTenantCheckoutForMember() {
    // Arrange
    when(subjectResolver.resolveSubject(TENANT_KEY, userId))
        .thenReturn(new SubscriptionSubject(SubjectType.TENANT, TENANT_KEY));
    when(jwt.getClaimAsStringList("authorities")).thenReturn(List.of("MEMBER"));

    // Act & Assert
    assertThatThrownBy(() -> resource.createCheckoutForMe(request, jwt))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(subscriptionService);
  }

  @Test
  @DisplayName("Should allow personal (USER-scoped) checkout for a MEMBER")
  void shouldAllowPersonalCheckoutForMember() {
    // Arrange
    when(subjectResolver.resolveSubject(TENANT_KEY, userId))
        .thenReturn(new SubscriptionSubject(SubjectType.USER, userId.toString()));
    when(subscriptionService.createCheckoutSessionForSubject(eq(TENANT_KEY), eq(userId), any()))
        .thenReturn(new SubscriptionDtos.CheckoutSessionResponse("https://checkout"));

    // Act
    final var response = resource.createCheckoutForMe(request, jwt);

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().checkoutUrl()).isEqualTo("https://checkout");
    verify(subscriptionService).createCheckoutSessionForSubject(eq(TENANT_KEY), eq(userId), any());
  }

  @Test
  @DisplayName("Should allow tenant-scoped checkout for a TENANT_OWNER")
  void shouldAllowTenantCheckoutForOwner() {
    // Arrange
    when(subjectResolver.resolveSubject(TENANT_KEY, userId))
        .thenReturn(new SubscriptionSubject(SubjectType.TENANT, TENANT_KEY));
    when(jwt.getClaimAsStringList("authorities")).thenReturn(List.of("MEMBER", "TENANT_OWNER"));
    when(subscriptionService.createCheckoutSessionForSubject(eq(TENANT_KEY), eq(userId), any()))
        .thenReturn(new SubscriptionDtos.CheckoutSessionResponse("https://checkout"));

    // Act
    final var response = resource.createCheckoutForMe(request, jwt);

    // Assert
    assertThat(response.getBody()).isNotNull();
    verify(subscriptionService).createCheckoutSessionForSubject(eq(TENANT_KEY), eq(userId), any());
  }
}
