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

package com.iqkv.foundation.billingservice.infrastructure.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * MyBatis type handler for PostgreSQL UUID type.
 * Maps {@link java.util.UUID} to JDBC {@code OTHER}.
 */
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(final PreparedStatement ps, final int i,
                                  final UUID parameter, final JdbcType jdbcType)
      throws SQLException {
    ps.setObject(i, parameter, Types.OTHER);
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final String columnName)
      throws SQLException {
    return (UUID) rs.getObject(columnName);
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final int columnIndex)
      throws SQLException {
    return (UUID) rs.getObject(columnIndex);
  }

  @Override
  public UUID getNullableResult(final CallableStatement cs, final int columnIndex)
      throws SQLException {
    return (UUID) cs.getObject(columnIndex);
  }
}
