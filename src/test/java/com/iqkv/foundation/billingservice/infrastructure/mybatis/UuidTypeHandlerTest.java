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

package com.iqkv.foundation.billingservice.infrastructure.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UuidTypeHandler Unit Tests")
class UuidTypeHandlerTest {

  private UuidTypeHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UuidTypeHandler();
  }

  @Test
  @DisplayName("Should set UUID parameter on PreparedStatement using Types.OTHER")
  void shouldSetNonNullParameter() throws SQLException {
    // Arrange
    final var ps = mock(PreparedStatement.class);
    final var uuid = UUID.randomUUID();

    // Act
    handler.setNonNullParameter(ps, 1, uuid, JdbcType.OTHER);

    // Assert
    verify(ps).setObject(1, uuid, Types.OTHER);
  }

  @Test
  @DisplayName("Should get UUID from ResultSet by column name")
  void shouldGetNullableResultByColumnName() throws SQLException {
    // Arrange
    final var rs = mock(ResultSet.class);
    final var uuid = UUID.randomUUID();
    when(rs.getObject("id")).thenReturn(uuid);

    // Act
    final UUID result = handler.getNullableResult(rs, "id");

    // Assert
    assertThat(result).isEqualTo(uuid);
  }

  @Test
  @DisplayName("Should return null from ResultSet by column name when value is null")
  void shouldGetNullableResultByColumnNameWhenNull() throws SQLException {
    // Arrange
    final var rs = mock(ResultSet.class);
    when(rs.getObject("id")).thenReturn(null);

    // Act
    final UUID result = handler.getNullableResult(rs, "id");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should get UUID from ResultSet by column index")
  void shouldGetNullableResultByColumnIndex() throws SQLException {
    // Arrange
    final var rs = mock(ResultSet.class);
    final var uuid = UUID.randomUUID();
    when(rs.getObject(1)).thenReturn(uuid);

    // Act
    final UUID result = handler.getNullableResult(rs, 1);

    // Assert
    assertThat(result).isEqualTo(uuid);
  }

  @Test
  @DisplayName("Should return null from ResultSet by column index when value is null")
  void shouldGetNullableResultByColumnIndexWhenNull() throws SQLException {
    // Arrange
    final var rs = mock(ResultSet.class);
    when(rs.getObject(2)).thenReturn(null);

    // Act
    final UUID result = handler.getNullableResult(rs, 2);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should get UUID from CallableStatement by column index")
  void shouldGetNullableResultFromCallableStatement() throws SQLException {
    // Arrange
    final var cs = mock(CallableStatement.class);
    final var uuid = UUID.randomUUID();
    when(cs.getObject(1)).thenReturn(uuid);

    // Act
    final UUID result = handler.getNullableResult(cs, 1);

    // Assert
    assertThat(result).isEqualTo(uuid);
  }

  @Test
  @DisplayName("Should return null from CallableStatement when value is null")
  void shouldGetNullableResultFromCallableStatementWhenNull() throws SQLException {
    // Arrange
    final var cs = mock(CallableStatement.class);
    when(cs.getObject(1)).thenReturn(null);

    // Act
    final UUID result = handler.getNullableResult(cs, 1);

    // Assert
    assertThat(result).isNull();
  }
}
