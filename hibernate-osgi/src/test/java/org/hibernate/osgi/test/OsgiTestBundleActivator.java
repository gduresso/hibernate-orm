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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.osgi.test.entity.DataPoint;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Brett Meyer
 */
public class OsgiTestBundleActivator implements BundleActivator {
	
	@Override
	public void start(BundleContext context) throws Exception {
		ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
		PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );
		EntityManagerFactory entityManagerFactory = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
		assertNotNull( entityManagerFactory );
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		assertNotNull( entityManager );
		
		DataPoint dp = new DataPoint();
        dp.setName( "Brett" );
        entityManager.getTransaction().begin();
        entityManager.persist( dp );
        entityManager.getTransaction().commit();
        entityManager.clear();
        
        entityManager.getTransaction().begin();
        List<DataPoint> results = entityManager.createQuery( "from DataPoint" ).getResultList();
        assertEquals(results.size(), 1);
        assertEquals("Brett", results.get(0).getName());
        entityManager.getTransaction().commit();
        entityManager.close();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
	}
}
