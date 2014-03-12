/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.RowSelection;

/**
 * LIMIT clause handler compatible with SQL Server 2012 and later.
 * 
 * @author Ivan Ermolaev
 * @author Deven Phillips (deven dot phillips at gmail dot com)
 */
public class SQLServer2012LimitHandler extends AbstractLimitHandler {

	// True if offset greater than 0.
	private boolean hasOffset = false;

	/**
	 * Constructs a SQLServer2012LimitHandler
	 * 
	 * @param sql The SQL
	 * @param selection The row selection options
	 */
	public SQLServer2012LimitHandler(String sql, RowSelection selection) {
		super( sql, selection );
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		// Our dialect paginated results aren't zero based. The first row should get the number 1 and so on
		return zeroBasedFirstResult + 1;
	}

	/**
	 * Add a LIMIT clause to the given SQL SELECT. Adds following clause to the SQL:
	 * 
	 * <pre>
	 * offset ? rows fetch next ? rows only
	 * </pre>
	 * 
	 * When first row isn't specified offset would be set to {@literal 0}
	 * 
	 * @return A new SQL statement with the LIMIT clause applied.
	 */
	@Override
	public String getProcessedSql() {
		final StringBuilder sb = new StringBuilder( sql );
		if ( sb.charAt( sb.length() - 1 ) == ';' ) {
			sb.setLength( sb.length() - 1 );
		}

		if ( LimitHelper.hasFirstRow( selection ) ) {
			hasOffset = true;
			// Wrap the query within a with statement:
			sb.append( " offset ? rows fetch next ? rows only" );
		}
		else {
			sb.append( " offset 0 rows fetch next ? rows only" );
		}

		return sb.toString();
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(PreparedStatement statement, int index) throws SQLException {
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(PreparedStatement statement, int index) throws SQLException {
		return hasOffset ? 1 : 0;
	}
}
