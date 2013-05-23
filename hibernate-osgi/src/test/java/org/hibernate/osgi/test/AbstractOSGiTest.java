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
package org.hibernate.osgi.test;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Brett Meyer
 */
public abstract class AbstractOSGiTest {
	
	@Inject
    protected BundleContext context;
	
	@Configuration
	public Option[] config() {

		return options(
				// TODO: NOT WORKING
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
				
				// JTA
				mavenBundle( "org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1" ),

				// JPA
				mavenBundle( "org.hibernate.javax.persistence", "hibernate-jpa-2.1-api", "1.0.0-SNAPSHOT" ),

				mavenBundle( "commons-collections", "commons-collections", "3.2.1" ),
				mavenBundle( "commons-pool", "commons-pool", "1.5.4" ),
				mavenBundle( "commons-dbcp", "commons-dbcp", "1.4" ),
				mavenBundle( "commons-lang", "commons-lang", "2.6" ),
				wrappedBundle( mavenBundle( "net.sourceforge.serp", "serp", "1.13.1" ) ),

				mavenBundle( "com.h2database", "h2", "1.3.170" ),

				// These do not natively support OSGi, so using 3rd party bundles.
				mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr", "2.7.7_5" ),
				mavenBundle( "org.jboss.javassist", "com.springsource.javassist", "3.15.0.GA" ),
				mavenBundle( "org.apache.servicemix.specs", "org.apache.servicemix.specs.jsr303-api-1.0.0", "2.2.0" ),
				mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant", "1.8.2_2" ),
				mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.dom4j", "1.6.1_5" ),

				// These do not natively support OSGi, so wrap with BND.
				wrappedBundle( mavenBundle( "org.hibernate.common", "hibernate-commons-annotations", "4.0.2.Final" ) ),
				wrappedBundle( mavenBundle( "org.jboss", "jandex", "1.1.0.Alpha1" ) ),

				mavenBundle( "com.fasterxml", "classmate", "0.5.4" ),
				mavenBundle( "org.jboss.logging", "jboss-logging", "3.1.0.GA" ),

				mavenBundle( "org.hibernate", "hibernate-core", "4.3.0-SNAPSHOT" ),
				mavenBundle( "org.hibernate", "hibernate-entitymanager", "4.3.0-SNAPSHOT" ),
				mavenBundle( "org.hibernate", "hibernate-osgi", "4.3.0-SNAPSHOT" ),

				junitBundles() );
	}
	
	// unmanaged Native
	protected Session getSession() {
		ServiceReference sr = context.getServiceReference( SessionFactory.class.getName() );
		SessionFactory sf = (SessionFactory) context.getService( sr );
		assertNotNull( sf );
		Session s = sf.openSession();
		assertNotNull( s );
		return s;
	}

	// unmanaged JPA
	protected EntityManager getEntityManager() {
		ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
		PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );
		EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
		assertNotNull( emf );
		EntityManager em = emf.createEntityManager();
		assertNotNull( em );
		return em;
	}

}
