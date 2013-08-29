/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.schemaupdate;

import static org.junit.Assert.assertEquals;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataBuilderImpl;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.junit.Test;

/**
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
public class MigrationTest extends BaseUnitTestCase {
	@Test
	public void testSimpleColumnAddition() {
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry();
		
		String resource1 = "org/hibernate/test/schemaupdate/1_Version.hbm.xml";
		String resource2 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";

		MetadataSources sources = new MetadataSources( serviceRegistry );
		sources.addResource( resource1 );
		MetadataImplementor metadata = (MetadataImplementor) new MetadataBuilderImpl(sources).build();
		new SchemaExport( metadata ).execute( false, true, true, false );

		SchemaUpdate v1schemaUpdate = new SchemaUpdate( metadata );
		v1schemaUpdate.execute( true, true );

		assertEquals( 0, v1schemaUpdate.getExceptions().size() );

		sources = new MetadataSources( serviceRegistry );
		sources.addResource( resource2 );
		metadata = (MetadataImplementor) new MetadataBuilderImpl(sources).build();

		SchemaUpdate v2schemaUpdate = new SchemaUpdate( metadata );
		v2schemaUpdate.execute( true, true );
		assertEquals( 0, v2schemaUpdate.getExceptions().size() );
		
		new SchemaExport( metadata ).drop( false, true );
	}
	
	/**
	 * 3_Version.hbm.xml contains a named unique constraint and an un-named
	 * unique constraint (will receive a randomly-generated name).  Create
	 * the original schema with 2_Version.hbm.xml.  Then, run SchemaUpdate
	 * TWICE using 3_Version.hbm.xml.  Neither RECREATE_QUIETLY nor SKIP should
	 * generate any exceptions.
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-8162" )
	public void testConstraintUpdate() {
		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY, true);
		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY, true);
		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.SKIP, false);
	}
	
	private void doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy strategy, boolean uniqueConstraintExpected) {
		// original
		String resource1 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";
		// adds unique constraint
		String resource2 = "org/hibernate/test/schemaupdate/3_Version.hbm.xml";
		
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry();
		MetadataSources sources = new MetadataSources( serviceRegistry );
		sources.addResource( resource1 );
		MetadataImplementor metadata = (MetadataImplementor) new MetadataBuilderImpl(sources).build();
		new SchemaExport( metadata ).execute( false, true, true, false );

		// adds unique constraint
		StandardServiceRegistryBuilder sbBuilder = new StandardServiceRegistryBuilder();
		sbBuilder.applySetting( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
		serviceRegistry = sbBuilder.build();
		sources = new MetadataSources( serviceRegistry );
		sources.addResource( resource2 );
		metadata = (MetadataImplementor) new MetadataBuilderImpl(sources).build();
		SchemaUpdate v2schemaUpdate = new SchemaUpdate( metadata );
		v2schemaUpdate.execute( true, true );
		assertEquals( 0, v2schemaUpdate.getExceptions().size() );

		SchemaUpdate v3schemaUpdate = new SchemaUpdate( metadata );
		v3schemaUpdate.execute( true, true );
		assertEquals( 0, v3schemaUpdate.getExceptions().size() );
		
		new SchemaExport( metadata ).drop( false, true );
	}

}

