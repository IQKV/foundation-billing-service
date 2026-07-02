package com.iqkv.foundation.billingservice.plan;

/**
 * Response payload for a single plan with full details for pricing pages.
 *
 * @param planCode      unique plan identifier
 * @param displayName   human-readable plan name
 * @param description   plan description for checkout and pricing pages
 * @param billingPeriod billing frequency (MONTHLY or ANNUAL)
 * @param priceMinor    price in minor currency units (e.g. cents for USD);
 *                      for PER_SEAT plans this is the per-seat unit price
 * @param currency      ISO 4217 currency code
 * @param entitlement   plan entitlement
 * @param scope         plan scope (TENANT or USER)
 * @param active        whether the plan is visible
 * @param pricingModel  pricing mode — {@code FLAT} or {@code PER_SEAT}; never null
 */
public record PublicPlanEntry(
    String planCode,
    String displayName,
    String description,
    String billingPeriod,
    Integer priceMinor,
    String currency,
    PlanEntitlement entitlement,
    String scope,
    Boolean active,
    PricingModel pricingModel
) {
}
