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
package org.hibernate.tool.hbm2ddl;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;

/**
 * @author Brett Meyer
 */
public class DefaultConstraintImplicitNamingStrategy implements ConstraintImplicitNamingStrategy {

	public final static DefaultConstraintImplicitNamingStrategy INSTANCE = new DefaultConstraintImplicitNamingStrategy();

	@Override
	public String generateConstraintName(Constraint constraint) {
		StringBuilder sb = new StringBuilder( constraint.generatedConstraintNamePrefix() );
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.
		sb.append( "table`" + constraint.getTable().getName() + "`" );
		for ( Object obj : constraint.columnsAlphabetical() ) {
			Column column = (Column) obj;
			sb.append( "column`" + column.getName() + "`" );
		}
		return StringHelper.md5HashBase35( sb.toString() );
	}

}
