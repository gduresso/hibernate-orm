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
package org.hibernate.test.naming;

import static org.junit.Assert.fail;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.UniqueConstraint;

import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@TestForIssue(jiraKey = "HHH-1904")
public class NameLengthTest extends BaseCoreFunctionalTestCase {
	
	@Test
	public void testNameLength() {
		// If truncation failed, the constraints won't exist.  Simply ensure
		// we get catch the expected exception.
		
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( new DataPoint( "foo", "foo", "foo", "foo", "foo", "foo" ) );
		s.getTransaction().commit();
		s.close();
		
		testUniqueConstraintException( "foo", "foo1", "foo1", "foo1", "foo1", "foo1" );
		testUniqueConstraintException( "foo2", "foo", "foo2", "foo2", "foo2", "foo2" );
		testUniqueConstraintException( "foo3", "foo3", "foo", "foo3", "foo3", "foo3" );
		testUniqueConstraintException( "foo4", "foo4", "foo4", "foo", "foo4", "foo4" );
		testUniqueConstraintException( "foo5", "foo5", "foo5", "foo5", "foo", "foo5" );
		testUniqueConstraintException( "foo6", "foo6", "foo6", "foo6", "foo6", "foo" );
	}
	
	private void testUniqueConstraintException(String s1, String s2,
			String s3, String s4, String s5, String s6) {
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( new DataPoint( s1, s2, s3, s4, s5, s6 ) );
		try {
			s.getTransaction().commit();
		}
		catch ( ConstraintViolationException e ) {
			// expected
			s.getTransaction().rollback();
			return;
		}
		finally {
			s.close();
		}
		
		fail( "The expected unique constraint exception did not occur.  The constraint may not have been created." );
	}
	
	@Override
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class<?>[] { DataPoint.class };
	}
	
	@Entity
	@javax.persistence.Table(name = "ThisTableHasAnLongEnoughName", uniqueConstraints = {
			@UniqueConstraint(name = "fooAUnique", columnNames = { "fooA" } ),
			@UniqueConstraint(name = "fooBConstraintPurposefullyHasAnIncrediblyLongName", columnNames = { "fooB" } ),
			@UniqueConstraint(name = "`fooDUnique`", columnNames = { "fooD" } ),
			@UniqueConstraint(name = "`fooEConstraintPurposefullyHasAnIncrediblyLongName`", columnNames = { "fooE" } ) } )
	public static class DataPoint {
		
		public DataPoint( String fooA, String fooB, String fooC,
				String thisColumnHasAnLongEnoughName,
				String fooD, String fooE ) {
			this.fooA = fooA;
			this.fooB = fooB;
			this.fooC = fooC;
			this.thisColumnHasAnLongEnoughName
					= thisColumnHasAnLongEnoughName;
			this.fooD = fooD;
			this.fooE = fooE;
		}
		
		@Id
		@GeneratedValue
		private long id;
		
		private String fooA;
		
		private String fooB;
		
		@Column( unique = true )
		private String fooC;
		
		@Column( unique = true )
		private String thisColumnHasAnLongEnoughName;
		
		private String fooD;
		
		private String fooE;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getFooA() {
			return fooA;
		}

		public void setFooA(String fooA) {
			this.fooA = fooA;
		}

		public String getFooB() {
			return fooB;
		}

		public void setFooB(String fooB) {
			this.fooB = fooB;
		}

		public String getFooC() {
			return fooC;
		}

		public void setFooC(String fooC) {
			this.fooC = fooC;
		}

		public String getThisColumnHasAnLongEnoughName() {
			return thisColumnHasAnLongEnoughName;
		}

		public void setThisColumnHasAnLongEnoughName(String thisColumnHasAnLongEnoughName) {
			this.thisColumnHasAnLongEnoughName = thisColumnHasAnLongEnoughName;
		}

		public String getFooD() {
			return fooD;
		}

		public void setFooD(String fooD) {
			this.fooD = fooD;
		}

		public String getFooE() {
			return fooE;
		}

		public void setFooE(String fooE) {
			this.fooE = fooE;
		}
	}
}