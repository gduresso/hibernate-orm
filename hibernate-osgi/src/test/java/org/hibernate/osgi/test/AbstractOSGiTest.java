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
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.osgi.test.entity.DataPoint;
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

		        // Log
		        mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
		        mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
		        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
		        
				// JTA
				mavenBundle( "org.jboss.spec.javax.interceptor", "jboss-interceptors-api_1.2_spec", "1.0.0.Alpha1" ).startLevel( 1 ),
				wrappedBundle( mavenBundle( "javax.enterprise", "cdi-api", "1.1-PFD" ) ).startLevel( 1 ),
				mavenBundle( "org.jboss.spec.javax.transaction", "jboss-transaction-api_1.2_spec", "1.0.0.Alpha1" ).startLevel( 1 ),

				// JPA
				mavenBundle( "org.hibernate.javax.persistence", "hibernate-jpa-2.1-api", "1.0.0-SNAPSHOT" ),

				mavenBundle( "commons-collections", "commons-collections" ),
				mavenBundle( "commons-pool", "commons-pool" ),
				mavenBundle( "commons-dbcp", "commons-dbcp" ),
				mavenBundle( "commons-lang", "commons-lang" ),
				wrappedBundle( mavenBundle( "net.sourceforge.serp", "serp" ) ),

				mavenBundle( "com.h2database", "h2", "1.3.170" ),

				mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr", "2.7.7_5" ),
				mavenBundle( "org.jboss.javassist", "com.springsource.javassist", "3.15.0.GA" ),
				mavenBundle( "org.apache.servicemix.specs", "org.apache.servicemix.specs.jsr303-api-1.0.0", "2.2.0" ),
				mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant", "1.8.2_2" ),
				wrappedBundle( mavenBundle( "dom4j", "dom4j", "1.6.1" ) ),
				
				wrappedBundle( mavenBundle( "org.hibernate.common", "hibernate-commons-annotations", "4.0.2.Final" ) ),
				wrappedBundle( mavenBundle( "org.jboss", "jandex", "1.1.0.Alpha1" ) ),

				mavenBundle( "com.fasterxml", "classmate", "0.5.4" ),
				mavenBundle( "org.jboss.logging", "jboss-logging", "3.1.0.GA" ),

				mavenBundle( "org.hibernate", "hibernate-core", "4.3.0-SNAPSHOT" ),
				mavenBundle( "org.hibernate", "hibernate-entitymanager", "4.3.0-SNAPSHOT" ),
				mavenBundle( "org.hibernate", "hibernate-osgi", "4.3.0-SNAPSHOT" ),

				junitBundles(),
				
				systemTimeout( 10 * 60 * 1000 ) );
	}
	
	// unmanaged Native
	protected Session getSession() {
		ClassLoader originalOsgiClassLoader = ClassLoaderHelper.overridenClassLoader;
		ClassLoaderHelper.overridenClassLoader = new OsgiTestClassLoader( originalOsgiClassLoader, DataPoint.class.getClassLoader() );
		
		ServiceReference sr = context.getServiceReference( SessionFactory.class.getName() );
		SessionFactory sf = (SessionFactory) context.getService( sr );
		assertNotNull( sf );
		Session s = sf.openSession();
		assertNotNull( s );
		return s;
	}

	// unmanaged JPA
	protected EntityManager getEntityManager() {
		ClassLoader originalOsgiClassLoader = ClassLoaderHelper.overridenClassLoader;
		ClassLoaderHelper.overridenClassLoader = new OsgiTestClassLoader( originalOsgiClassLoader, DataPoint.class.getClassLoader() );
		
		ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
		PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );
		EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
		assertNotNull( emf );
		EntityManager em = emf.createEntityManager();
		assertNotNull( em );
		return em;
	}
	
	/**
	 * The Pax Exam framework appears to hijack the "requesting bundle" when calling #getService with some sort of
	 * Pax-specific proxy.  It's ClassLoader does not appear to work as expected.  The test entities in hibernate-osgi
	 * were not found, even if they were temporarily in src/main/java.  For now, use this hack.  It overrides the
	 * static ClassLoader variable set when the hibernate-osgi bundle activates.
	 * 
	 * TODO: This is terrible and will only work with our 4.2/4.3 ClassLoaderHelper.overridenClassLoader band-aid.
	 * Discuss with the Pax dev team?
	 */
	public class OsgiTestClassLoader extends ClassLoader {
		private final ClassLoader originalOsgiClassLoader;
		private final ClassLoader thisClassLoader;
		
		public OsgiTestClassLoader( ClassLoader originalOsgiClassLoader, ClassLoader thisClassLoader ) {
			this.originalOsgiClassLoader = originalOsgiClassLoader;
			this.thisClassLoader = thisClassLoader;
		}
		
		@Override
		@SuppressWarnings("rawtypes")
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				final Class clazz = thisClassLoader.loadClass( name );
				if ( clazz != null ) {
					return clazz;
				}
			}
			catch ( Exception ignore ) {
			}
			
			return originalOsgiClassLoader.loadClass( name );
		}
		
		@Override
		public URL getResource(String name) {
			try {
				final URL resource = thisClassLoader.getResource( name );
				if ( resource != null ) {
					return resource;
				}
			}
			catch ( Exception ignore ) {
			}
			
			return originalOsgiClassLoader.getResource( name );
		}
		
		@Override
		public InputStream getResourceAsStream(String name) {
			try {
				final InputStream resource = thisClassLoader.getResourceAsStream( name );
				if ( resource != null ) {
					return resource;
				}
			}
			catch ( Exception ignore ) {
			}
			
			return originalOsgiClassLoader.getResourceAsStream( name );
		}
		
		@Override
		public Enumeration getResources(String name) throws java.io.IOException {
			try {
				final Enumeration<URL> resources = thisClassLoader.getResources( name );
				if ( resources != null ) {
					return resources;
				}
			}
			catch ( Exception ignore ) {
			}
			
			return originalOsgiClassLoader.getResources( name );
		}
	}

}
