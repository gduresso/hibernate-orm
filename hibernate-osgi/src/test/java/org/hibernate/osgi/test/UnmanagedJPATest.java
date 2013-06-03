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

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.osgi.test.entity.DataPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * @author Brett Meyer
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class) // restarts the OSGi framework for every test -- cleaner
public class UnmanagedJPATest extends AbstractOSGiTest {
 
    @Test
    public void testEntityManager() {
        EntityManager em = getEntityManager();
        
        DataPoint dp = new DataPoint();
        dp.setName( "Brett" );
        em.getTransaction().begin();
        em.persist( dp );
        em.getTransaction().commit();
        em.clear();
        
        em.getTransaction().begin();
        List<DataPoint> results = em.createQuery( "from DataPoint" ).getResultList();
        assertEquals(results.size(), 1);
        assertEquals("Brett", results.get(0).getName());
        em.getTransaction().commit();
        em.close();
    }
}
