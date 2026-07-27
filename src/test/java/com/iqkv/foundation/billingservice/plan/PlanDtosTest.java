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

package com.iqkv.foundation.billingservice.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Plan DTOs Unit Tests")
class PlanDtosTest {

  @Test
  @DisplayName("Should create PlanEntitlement with all fields")
  void shouldCreatePlanEntitlementWithAllFields() {
    // Arrange
    final int maxUsers = 10;
    final int maxProjects = 5;
    final var features = Map.of(
        "priority_support", new PlanFeature("priority_support", "Priority Support", "true", "Access to priority support"),
        "advanced_analytics", new PlanFeature("advanced_analytics", "Advanced Analytics", "true", "Detailed analytics")
    );
    final String pricingModel = "PER_SEAT";

    // Act
    final var entitlement = new PlanEntitlement(maxUsers, maxProjects, features, pricingModel);

    // Assert
    assertThat(entitlement.maxUsers()).isEqualTo(maxUsers);
    assertThat(entitlement.maxProjects()).isEqualTo(maxProjects);
    assertThat(entitlement.features()).containsExactlyInAnyOrderEntriesOf(features);
    assertThat(entitlement.pricingModel()).isEqualTo(pricingModel);
  }

  @Test
  @DisplayName("Should create PlanEntitlement with NONE fallback")
  void shouldCreatePlanEntitlementWithNoneFallback() {
    // Arrange & Act
    final var entitlement = PlanEntitlement.NONE;

    // Assert
    assertThat(entitlement.maxUsers()).isEqualTo(1);
    assertThat(entitlement.maxProjects()).isEqualTo(1);
    assertThat(entitlement.features()).isEmpty();
    assertThat(entitlement.pricingModel()).isNull();
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when maxUsers is negative")
  void shouldThrowIllegalArgumentExceptionWhenMaxUsersNegative() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new PlanEntitlement(-1, 1, Map.of(), "FLAT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxUsers must be >= 0");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when maxProjects is negative")
  void shouldThrowIllegalArgumentExceptionWhenMaxProjectsNegative() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new PlanEntitlement(1, -1, Map.of(), "FLAT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxProjects must be >= 0");
  }

  @Test
  @DisplayName("Should return true for has() when feature exists and is enabled")
  void shouldReturnTrueForHasWhenFeatureExistsAndEnabled() {
    // Arrange
    final var features = Map.of(
        "priority_support", new PlanFeature("priority_support", "Priority Support", "true", "Access to priority support")
    );
    final var entitlement = new PlanEntitlement(10, 5, features, "FLAT");

    // Act
    final boolean hasFeature = entitlement.has("priority_support");

    // Assert
    assertThat(hasFeature).isTrue();
  }

  @Test
  @DisplayName("Should return false for has() when feature doesn't exist")
  void shouldReturnFalseForHasWhenFeatureDoesntExist() {
    // Arrange
    final var features = Map.of(
        "priority_support",
        new PlanFeature(
            "priority_support",
            "Priority Support",
            "true",
            "Access to priority support"
        )
    );
    final var entitlement = new PlanEntitlement(10, 5, features, "FLAT");

    // Act
    final boolean hasFeature = entitlement.has("unknown_feature");

    // Assert
    assertThat(hasFeature).isFalse();
  }

  @Test
  @DisplayName("Should return false for has() when feature exists but is disabled")
  void shouldReturnFalseForHasWhenFeatureExistsButDisabled() {
    // Arrange
    final var features = Map.of(
        "priority_support",
        new PlanFeature(
            "priority_support",
            "Priority Support",
            "false",
            "Access to priority support"
        )
    );
    final var entitlement = new PlanEntitlement(10, 5, features, "FLAT");

    // Act
    final boolean hasFeature = entitlement.has("priority_support");

    // Assert
    assertThat(hasFeature).isFalse();
  }

  @Test
  @DisplayName("Should return false for has() with null or blank code")
  void shouldReturnFalseForHasWithNullOrBlankCode() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), "FLAT");

    // Act & Assert
    assertThat(entitlement.has(null)).isFalse();
    assertThat(entitlement.has("")).isFalse();
    assertThat(entitlement.has("   ")).isFalse();
  }

  @Test
  @DisplayName("Should return true for isPerSeat() when pricingModel is PER_SEAT")
  void shouldReturnTrueForIsPerSeatWhenPricingModelIsPerSeat() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), "PER_SEAT");

    // Act
    final boolean isPerSeat = entitlement.isPerSeat();

    // Assert
    assertThat(isPerSeat).isTrue();
  }

  @Test
  @DisplayName("Should return false for isPerSeat() when pricingModel is FLAT")
  void shouldReturnFalseForIsPerSeatWhenPricingModelIsFlat() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), "FLAT");

    // Act
    final boolean isPerSeat = entitlement.isPerSeat();

    // Assert
    assertThat(isPerSeat).isFalse();
  }

  @Test
  @DisplayName("Should return false for isPerSeat() when pricingModel is null")
  void shouldReturnFalseForIsPerSeatWhenPricingModelIsNull() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), null);

    // Act
    final boolean isPerSeat = entitlement.isPerSeat();

    // Assert
    assertThat(isPerSeat).isFalse();
  }

  @Test
  @DisplayName("Should return true for isFlatPricing() when pricingModel is FLAT")
  void shouldReturnTrueForIsFlatPricingWhenPricingModelIsFlat() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), "FLAT");

    // Act
    final boolean isFlat = entitlement.isFlatPricing();

    // Assert
    assertThat(isFlat).isTrue();
  }

  @Test
  @DisplayName("Should return false for isFlatPricing() when pricingModel is PER_SEAT")
  void shouldReturnFalseForIsFlatPricingWhenPricingModelIsPerSeat() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), "PER_SEAT");

    // Act
    final boolean isFlat = entitlement.isFlatPricing();

    // Assert
    assertThat(isFlat).isFalse();
  }

  @Test
  @DisplayName("Should return true for isFlatPricing() when pricingModel is null")
  void shouldReturnTrueForIsFlatPricingWhenPricingModelIsNull() {
    // Arrange
    final var entitlement = new PlanEntitlement(10, 5, Map.of(), null);

    // Act
    final boolean isFlat = entitlement.isFlatPricing();

    // Assert
    assertThat(isFlat).isTrue();
  }

  @Test
  @DisplayName("Should create PublicPlanEntry with all fields")
  void shouldCreatePublicPlanEntryWithAllFields() {
    // Arrange
    final String planCode = "pro-monthly";
    final String displayName = "Pro Monthly";
    final String description = "Pro plan for monthly billing";
    final String billingPeriod = "MONTHLY";
    final Integer priceMinor = 999;
    final String currency = "USD";
    final var entitlement = PlanEntitlement.NONE;
    final String scope = "TENANT";
    final Boolean active = true;
    final PricingModel pricingModel = PricingModel.FLAT;

    // Act
    final var entry = new PublicPlanEntry(
        planCode, displayName, description, billingPeriod, priceMinor, currency,
        entitlement, scope, active, pricingModel
    );

    // Assert
    assertThat(entry.planCode()).isEqualTo(planCode);
    assertThat(entry.displayName()).isEqualTo(displayName);
    assertThat(entry.description()).isEqualTo(description);
    assertThat(entry.billingPeriod()).isEqualTo(billingPeriod);
    assertThat(entry.priceMinor()).isEqualTo(priceMinor);
    assertThat(entry.currency()).isEqualTo(currency);
    assertThat(entry.entitlement()).isEqualTo(entitlement);
    assertThat(entry.scope()).isEqualTo(scope);
    assertThat(entry.active()).isEqualTo(active);
    assertThat(entry.pricingModel()).isEqualTo(pricingModel);
  }

  @Test
  @DisplayName("Should support record equality for PublicPlanEntry")
  void shouldSupportRecordEqualityForPublicPlanEntry() {
    // Arrange
    final var entitlement = PlanEntitlement.NONE;
    final var entry1 = new PublicPlanEntry(
        "pro-monthly", "Pro Monthly", "Description", "MONTHLY", 999, "USD",
        entitlement, "TENANT", true, PricingModel.FLAT
    );
    final var entry2 = new PublicPlanEntry(
        "pro-monthly", "Pro Monthly", "Description", "MONTHLY", 999, "USD",
        entitlement, "TENANT", true, PricingModel.FLAT
    );

    // Assert
    assertThat(entry1).isEqualTo(entry2);
    assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString for PublicPlanEntry")
  void shouldHaveMeaningfulToStringForPublicPlanEntry() {
    // Arrange
    final var entitlement = PlanEntitlement.NONE;
    final var entry = new PublicPlanEntry(
        "pro-monthly", "Pro Monthly", "Description", "MONTHLY", 999, "USD",
        entitlement, "TENANT", true, PricingModel.FLAT
    );

    // Act
    final String toString = entry.toString();

    // Assert
    assertThat(toString).contains("PublicPlanEntry");
    assertThat(toString).contains("pro-monthly");
    assertThat(toString).contains("Pro Monthly");
    assertThat(toString).contains("MONTHLY");
  }

  @Test
  @DisplayName("Should create PlanFeature with all fields")
  void shouldCreatePlanFeatureWithAllFields() {
    // Arrange
    final String code = "priority_support";
    final String title = "Priority Support";
    final String value = "true";
    final String description = "Access to priority support channel";

    // Act
    final var feature = new PlanFeature(code, title, value, description);

    // Assert
    assertThat(feature.code()).isEqualTo(code);
    assertThat(feature.title()).isEqualTo(title);
    assertThat(feature.value()).isEqualTo(value);
    assertThat(feature.description()).isEqualTo(description);
  }

  @Test
  @DisplayName("Should return true for isEnabled() when value is true (case-insensitive)")
  void shouldReturnTrueForIsEnabledWhenValueIsTrue() {
    // Arrange
    final var feature1 = new PlanFeature("f1", "F1", "true", null);
    final var feature2 = new PlanFeature("f2", "F2", "TRUE", null);
    final var feature3 = new PlanFeature("f3", "F3", "True", null);

    // Act
    final boolean enabled1 = feature1.isEnabled();
    final boolean enabled2 = feature2.isEnabled();
    final boolean enabled3 = feature3.isEnabled();

    // Assert
    assertThat(enabled1).isTrue();
    assertThat(enabled2).isTrue();
    assertThat(enabled3).isTrue();
  }

  @Test
  @DisplayName("Should return false for isEnabled() when value is not true")
  void shouldReturnFalseForIsEnabledWhenValueIsNotTrue() {
    // Arrange
    final var feature1 = new PlanFeature("f1", "F1", "false", null);
    final var feature2 = new PlanFeature("f2", "F2", "10", null);
    final var feature3 = new PlanFeature("f3", "F3", null, null);

    // Act
    final boolean enabled1 = feature1.isEnabled();
    final boolean enabled2 = feature2.isEnabled();
    final boolean enabled3 = feature3.isEnabled();

    // Assert
    assertThat(enabled1).isFalse();
    assertThat(enabled2).isFalse();
    assertThat(enabled3).isFalse();
  }

  @Test
  @DisplayName("Should support record equality for PlanFeature")
  void shouldSupportRecordEqualityForPlanFeature() {
    // Arrange
    final var feature1 = new PlanFeature("f1", "Feature 1", "true", "Description");
    final var feature2 = new PlanFeature("f1", "Feature 1", "true", "Description");

    // Assert
    assertThat(feature1).isEqualTo(feature2);
    assertThat(feature1.hashCode()).isEqualTo(feature2.hashCode());
  }

  @Test
  @DisplayName("Should have meaningful toString for PlanFeature")
  void shouldHaveMeaningfulToStringForPlanFeature() {
    // Arrange
    final var feature = new PlanFeature("priority_support", "Priority Support", "true", "Description");

    // Act
    final String toString = feature.toString();

    // Assert
    assertThat(toString).contains("PlanFeature");
    assertThat(toString).contains("priority_support");
    assertThat(toString).contains("Priority Support");
    assertThat(toString).contains("true");
  }
}
