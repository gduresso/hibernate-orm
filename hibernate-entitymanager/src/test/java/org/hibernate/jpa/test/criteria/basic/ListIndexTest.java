/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.criteria.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Address;
import org.hibernate.jpa.test.metamodel.Address_;
import org.hibernate.jpa.test.metamodel.Phone;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Tests usage of {@link ListJoin#index()}
 *
 * @author Brett Meyer
 */
public class ListIndexTest extends AbstractMetamodelSpecificTest {
	
	@Test
	@TestForIssue(jiraKey = "HHH-8404")
	public void testListIndex() {
		EntityManager em = getOrCreateEntityManager();
		
		em.getTransaction().begin();
		Address address = new Address();
		address.setId( "a1" );
		Phone phone1 = new Phone();
		phone1.setId( "p1" );
		phone1.setAddress( address );
		Phone phone2 = new Phone();
		phone2.setId( "p2" );
		phone2.setAddress( address );
		address.getPhones().add( phone1 );
		address.getPhones().add( phone2 );
		em.persist( phone1 );
		em.persist( phone2 );
		em.persist( address );
		em.getTransaction().commit();
		em.clear();
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Address> criteria = cb.createQuery( Address.class );
		Root<Address> addressRoot = criteria.from( Address.class );
		ListJoin<Address, Phone> phones = addressRoot.join( Address_.phones );
		criteria.where( cb.ge( phones.index(), 1 ) );
		Address result = em.createQuery( criteria ).getSingleResult();

		assertNotNull( result );
		assertNotNull( result.getPhones() );
		assertEquals( 1, result.getPhones().size() );
	}
}
