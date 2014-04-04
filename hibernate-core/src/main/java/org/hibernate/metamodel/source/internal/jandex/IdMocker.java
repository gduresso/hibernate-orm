/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbGeneratedValue;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 */
public class IdMocker extends PropertyMocker {
	private final JaxbId id;

	IdMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, JaxbId id) {
		super( indexBuilder, classInfo, defaults );
		this.id = id;
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return id;
	}

	@Override
	protected void processExtra() {
		create( ID );
		parseColumn( id.getColumn(), getTarget() );
		parseGeneratedValue( id.getGeneratedValue(), getTarget() );
		parseTemporalType( id.getTemporal(), getTarget() );
	}

	private void parseGeneratedValue(JaxbGeneratedValue generatedValue, AnnotationTarget target) {
		if ( generatedValue == null ) {
			return;
		}
		
		// @GeneratedValue(generator = "...")
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.enumValue( "strategy", GENERATION_TYPE, generatedValue.getStrategy(), annotationValueList );
		MockHelper.stringValue( "generator", generatedValue.getGenerator(), annotationValueList );
		create( GENERATED_VALUE, target, annotationValueList );
		
		// TODO: Assumes all generators are generic.  How to check to see if it's custom?
		if (! StringHelper.isEmpty( generatedValue.getGenerator() )) {
			// @GenericGenerator(name = "...", strategy = "...")
			annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.stringValue( "name", generatedValue.getGenerator(), annotationValueList );
			MockHelper.stringValue( "strategy", generatedValue.getGenerator(), annotationValueList );
			create( HibernateDotNames.GENERIC_GENERATOR, target, annotationValueList );
		}
	}
}
