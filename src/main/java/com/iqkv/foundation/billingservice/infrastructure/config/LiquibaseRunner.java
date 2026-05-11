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

import java.sql.Connection;
import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs Liquibase migrations on application startup using the Liquibase Java API directly.
 *
 * <p>Spring Boot's {@code LiquibaseAutoConfiguration} does not work with Liquibase 5.x
 * because the Spring integration was restructured in that release. This runner replicates
 * the same behaviour by calling the Liquibase API directly, which is the same approach
 * used by {@code TenantLiquibaseRunner} in the IAM service.
 *
 * <p>Enabled by default; disable with {@code spring.liquibase.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = false)
public class LiquibaseRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(LiquibaseRunner.class);

  private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

  private final DataSource dataSource;

  public LiquibaseRunner(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    log.info("Running Liquibase migrations from {}", CHANGELOG);
    try (final Connection connection = dataSource.getConnection()) {
      final Database database = DatabaseFactory.getInstance()
          .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (final Liquibase liquibase = new Liquibase(
          CHANGELOG,
          new ClassLoaderResourceAccessor(),
          database)) {
        liquibase.update(new Contexts(), new LabelExpression());
      }
    }
    log.info("Liquibase migrations completed successfully");
  }
}
