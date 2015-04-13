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
package org.hibernate.osgi.test.client;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.integrator.spi.Integrator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

/**
 * @author Brett Meyer
 */
public class OsgiTestActivator implements BundleActivator {

	private TestService testService;

	@Override
	public void start(BundleContext context) throws Exception {
		
		final TestIntegrator integrator = new TestIntegrator();
		final TestStrategyRegistrationProvider strategyRegistrationProvider = new TestStrategyRegistrationProvider();
		final TestTypeContributor typeContributor = new TestTypeContributor();
		
		// register example extension point services
		context.registerService( Integrator.class, integrator, new Hashtable() );
		context.registerService( StrategyRegistrationProvider.class, strategyRegistrationProvider, new Hashtable() );
		context.registerService( TypeContributor.class, typeContributor, new Hashtable() );

		// register the test result service
		testService = new TestServiceImpl(context, integrator, strategyRegistrationProvider, typeContributor);
		context.registerService( TestService.class, testService, new Hashtable() );

//        DataPoint dp = new DataPoint();
//        dp.setName( "Brett" );
//        testService.saveNative( dp );
//
//        dp = testService.getNative(dp.getId());
//        System.out.println(dp.getName());
//
//        dp.setName( "Brett2" );
//        testService.updateNative( dp );
//
//        dp = testService.getNative(dp.getId());
//        System.out.println(dp.getName());
//
//        testService.deleteNative();
//
//        dp = testService.getNative(dp.getId());
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
