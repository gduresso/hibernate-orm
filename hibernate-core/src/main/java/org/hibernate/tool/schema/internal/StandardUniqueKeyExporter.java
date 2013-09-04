/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Brett Meyer
 */
public class StandardUniqueKeyExporter implements Exporter<UniqueKey> {

	private final Dialect dialect;

	public StandardUniqueKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}
	
	@Override
	public String[] getSqlCreateStrings(UniqueKey uniqueKey, JdbcEnvironment jdbcEnvironment) {
		
		final UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy.interpret(
				jdbcEnvironment.getServiceRegistry().getService( ConfigurationService.class ).getSetting(
						AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY ) );
		
		if ( !dialect.hasAlterTable() || constraintMethod == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			return NO_COMMANDS;
		}
		
		final List<String> strings = new ArrayList<String>();
		// TODO: No concept of "quiet"
		if ( constraintMethod.equals( UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY ) ) {
			strings.add( dialect.getUniqueDelegate().getAlterTableToDropUniqueKeyCommand( uniqueKey ) );
		}
		strings.add( dialect.getUniqueDelegate().getAlterTableToAddUniqueKeyCommand( uniqueKey ) );
		return strings.toArray( new String[strings.size()] );
	}

	@Override
	public String[] getSqlDropStrings(UniqueKey uniqueKey, JdbcEnvironment jdbcEnvironment) {
		if ( !dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}
		return new String[] { dialect.getUniqueDelegate().getAlterTableToDropUniqueKeyCommand( uniqueKey ) };
	}

}
