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

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

import org.hibernate.osgi.test.entity.DataPoint;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * @author Brett Meyer
 */
public abstract class AbstractOSGiTest {
	
	@Inject
    protected BundleContext context;
	
	@Configuration
	public Option[] config() {
		
		URL cfgUrl = null;
		URL persistenceUrl = null;
		try {
			cfgUrl = new File("src/test/resources/hibernate.cfg.xml").toURI().toURL();
			persistenceUrl = new File("src/test/resources/META-INF/persistence.xml").toURI().toURL();
		}
		catch ( MalformedURLException e ) {
			fail( "Could not locate hibernate.cfg.xml and/or persistence.xml for the client bundle. " );
		}
		
		return options(

		        // Log
//		        mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
//		       mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
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

				// TODO: Should these be using mavenBundle?  Find a way to use the local projects?
				mavenBundle( "org.hibernate", "hibernate-core", "4.3.0-SNAPSHOT" ),
				mavenBundle( "org.hibernate", "hibernate-entitymanager", "4.3.0-SNAPSHOT" ),
				mavenBundle( "org.hibernate", "hibernate-osgi", "4.3.0-SNAPSHOT" ),
				
				streamBundle(org.ops4j.pax.tinybundles.core.TinyBundles.bundle()
						.add( DataPoint.class )
//						.add( "hibernate.cfg.xml", cfgUrl )
						.add( "META-INF/persistence.xml", persistenceUrl )
						.set( Constants.BUNDLE_SYMBOLICNAME, "Hibernate OSGi Test Bundle" )
						.set( Constants.EXPORT_PACKAGE, "org.hibernate.osgi.test,org.hibernate.osgi.test.entity" )
						.set( Constants.IMPORT_PACKAGE, "javax.persistence;version=\"2.1.0\",javax.persistence.spi;version=\"2.1.0\",org.hibernate,org.junit,org.osgi.framework" )
						.add( OsgiTestBundleActivator.class )
						.set( Constants.BUNDLE_ACTIVATOR, OsgiTestBundleActivator.class.getName() )
						.build()),

				junitBundles(),
				
				systemTimeout( 10 * 60 * 1000 ) );
	}

}
