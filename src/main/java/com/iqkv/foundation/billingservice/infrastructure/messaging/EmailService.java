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

package com.iqkv.foundation.billingservice.infrastructure.messaging;

import java.util.Locale;

import com.iqkv.foundation.billingservice.infrastructure.config.NotificationConfigurationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Orchestrates outbound email delivery.
 *
 * <p>This class owns two concerns:
 * <ol>
 *   <li><b>Rendering</b> — resolves the Thymeleaf template path and i18n subject key from
 *       {@link NotificationEventType}, renders the HTML body via {@link TemplateEngine}, and
 *       resolves the localised subject via {@link MessageSource}.</li>
 *   <li><b>Transport delegation</b> — hands the fully-assembled {@link EmailSendRequest} to the
 *       active {@link EmailSender} (SMTP or Resend), which is injected at startup based on
 *       {@code iqkv.notification.mail.provider}.</li>
 * </ol>
 *
 * <p>Switching providers requires only a configuration change — no code changes here.
 */
@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final EmailSender emailSender;
  private final TemplateEngine templateEngine;
  private final NotificationConfigurationProperties notificationProps;
  private final MessageSource messageSource;
  private final MeterRegistry meterRegistry;

  public EmailService(final EmailSender emailSender,
                      final TemplateEngine templateEngine,
                      final NotificationConfigurationProperties notificationProps,
                      final MessageSource messageSource,
                      final MeterRegistry meterRegistry) {
    this.emailSender = emailSender;
    this.templateEngine = templateEngine;
    this.notificationProps = notificationProps;
    this.messageSource = messageSource;
    this.meterRegistry = meterRegistry;
  }

  public void send(final NotificationEvent event) {
    final Locale locale = resolveLocale(event.getLocale());
    final String templateName = resolveTemplate(event.getType());
    final String subjectKey = resolveSubjectKey(event.getType());

    final Context ctx = new Context(locale);
    if (event.getPayload() != null) {
      event.getPayload().forEach(ctx::setVariable);
    }

    final String htmlBody = templateEngine.process(templateName, ctx);
    final String subject = messageSource.getMessage(subjectKey, null, locale);

    final EmailSendRequest request = new EmailSendRequest(
        notificationProps.mail().from(),
        notificationProps.mail().fromName(),
        notificationProps.mail().replyTo(),
        event.getRecipientEmail(),
        subject,
        htmlBody
    );

    try {
      emailSender.send(request);
      log.info("Email sent: provider={} type={} to={}",
          notificationProps.mail().provider(), event.getType(), event.getRecipientEmail());
      meterRegistry.counter("billing_emails_sent_total",
          "type", event.getType().name(), "status", "success").increment();
    } catch (final com.iqkv.foundation.billingservice.shared.exception.MessagingException e) {
      log.error("Failed to send email: type={} to={}", event.getType(), event.getRecipientEmail(), e);
      meterRegistry.counter("billing_emails_sent_total",
          "type", event.getType().name(), "status", "failure").increment();
      throw e;
    }
  }

  private Locale resolveLocale(final String localeTag) {
    if (localeTag != null && !localeTag.isBlank()) {
      return Locale.forLanguageTag(localeTag);
    }
    final String defaultLocale = notificationProps.defaultLocale();
    return defaultLocale != null ? Locale.forLanguageTag(defaultLocale) : Locale.ENGLISH;
  }

  private String resolveTemplate(final NotificationEventType type) {
    return switch (type) {
      case SUBSCRIPTION_ACTIVATED -> "email/billing/subscription-activated";
      case SUBSCRIPTION_UPDATED -> "email/billing/subscription-updated";
      case SUBSCRIPTION_CANCELLED -> "email/billing/subscription-cancelled";
      case TRIAL_ENDING -> "email/billing/trial-ending";
      case PAYMENT_OVERDUE -> "email/billing/payment-overdue";
      case PAYMENT_FAILED -> "email/billing/payment-failed";
      case INVOICE_PAID -> "email/billing/invoice-paid";
      case BILLING_UPDATED -> "email/billing/billing-updated";
      case ACCOUNT_SUSPENDED -> "email/billing/account-suspended";
      case REFUND_CREATED -> "email/billing/refund-created";
    };
  }

  private String resolveSubjectKey(final NotificationEventType type) {
    return switch (type) {
      case SUBSCRIPTION_ACTIVATED -> "email.subscription-activated.subject";
      case SUBSCRIPTION_UPDATED -> "email.subscription-updated.subject";
      case SUBSCRIPTION_CANCELLED -> "email.subscription-cancelled.subject";
      case TRIAL_ENDING -> "email.trial-ending.subject";
      case PAYMENT_OVERDUE -> "email.payment-overdue.subject";
      case PAYMENT_FAILED -> "email.payment-failed.subject";
      case INVOICE_PAID -> "email.invoice-paid.subject";
      case BILLING_UPDATED -> "email.billing-updated.subject";
      case ACCOUNT_SUSPENDED -> "email.account-suspended.subject";
      case REFUND_CREATED -> "email.refund-created.subject";
    };
  }
}
