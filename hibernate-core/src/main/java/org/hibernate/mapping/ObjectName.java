/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * Models multi-part names for tables, columns, constraints, etc.
 * 
 * TODO: At some point, this would probably be better off using a dynamic
 * collection of segments, rather than hard-coded 3 spots.  However, things get
 * complicated when, for example, a Table's "most important" segment is the 3rd
 * one.  For now, keep the legacy structure and revisit.
 * 
 * @author Brett Meyer
 */
public class ObjectName {
	public static final char DEFAULT_QUOTE = '`';

	// ex: Table catalog (optional)
	private String seg1;
	// ex: Table schema (optional)
	private String seg2;
	// ex: Table name (required)
	private String seg3;

	private String delimiter = ".";

	// Is the entire name quoted?
	private boolean quoted = false;

	// Are the individual pieces quoted?
	private boolean seg1Quoted = false;
	private boolean seg2Quoted = false;
	private boolean seg3Quoted = false;
	
	public ObjectName() {}

	public ObjectName(String text) {
		quoted = StringHelper.isQuoted( text );
		text = StringHelper.unquote( text );
		
		// schema.catalog.name
		if ( !text.contains( "." ) ) {
			delimiter = "_";
		}

		final String[] tokens = text.split( delimiter );

		String seg1 = null;
		String seg2 = null;
		String seg3;
		
		if ( tokens.length == 2 ) {
			seg2 = tokens[0];
			seg3 = tokens[1];
		}
		else if ( tokens.length == 3 ) {
			seg1 = tokens[0];
			seg2 = tokens[1];
			seg3 = tokens[2];
		}
		else {
			// Assume it's an explicit identifier given in a mapping.
			// TODO: For now, treating n >= 4 as one token. Could change to squash
			// the last n-2 entries, etc.
			seg3 = text;
		}

		init( seg1, seg2, seg3 );
	}

	public ObjectName(String seg1, String seg2, String seg3) {
		init( seg1, seg2, seg3 );
	}

	protected void init(String seg1, String seg2, String seg3) {
		seg1Quoted = StringHelper.isQuoted( seg1 );
		this.seg1 = StringHelper.unquote( seg1 );
		seg2Quoted = StringHelper.isQuoted( seg2 );
		this.seg2 = StringHelper.unquote( seg2 );
		seg3Quoted = StringHelper.isQuoted( seg3 );
		this.seg3 = StringHelper.unquote( seg3 );
	}

	public String quoted() {
		String seg1Formatted = formatSeg( seg1, seg1Quoted );
		String seg2Formatted = formatSeg( seg2, seg2Quoted );
		String seg3Formatted = formatSeg( seg3, seg3Quoted );
		return quoted( seg1Formatted, seg2Formatted, seg3Formatted,
				DEFAULT_QUOTE, DEFAULT_QUOTE);
	}

	public String quoted(Dialect dialect) {
		String seg1Formatted = formatSeg( seg1, seg1Quoted, dialect );
		String seg2Formatted = formatSeg( seg2, seg2Quoted, dialect );
		String seg3Formatted = formatSeg( seg3, seg3Quoted, dialect );
		return quoted( seg1Formatted, seg2Formatted, seg3Formatted,
				dialect.openQuote(), dialect.closeQuote() );
	}

	public String quoted(Dialect dialect,
			String defaultSeg1, String defaultSeg2) {		
		String seg1Formatted = seg1 != null ? seg1 : defaultSeg1;
		String seg2Formatted = seg2 != null ? seg2 : defaultSeg2;
		String seg3Formatted = seg3;

		seg1Formatted = formatSeg( seg1Formatted, seg1Quoted, dialect );
		seg2Formatted = formatSeg( seg2Formatted, seg2Quoted, dialect );
		seg3Formatted = formatSeg( seg3Formatted, seg3Quoted, dialect );
		
		return quoted( seg1Formatted, seg2Formatted, seg3Formatted,
				dialect.openQuote(), dialect.closeQuote() );
	}
	
	private String quoted(String seg1Formatted, String seg2Formatted,
			String seg3Formatted, char openQuote, char closeQuote) {
		StringBuilder buff = new StringBuilder( seg3Formatted );
		if ( seg2Formatted != null ) {
			buff.insert( 0, seg2Formatted + delimiter );
		}
		if ( seg1Formatted != null ) {
			buff.insert( 0, seg1Formatted + delimiter );
		}
		
		// allow segment quoting to override quotes around the entire identifier
		if ( quoted && !seg3Quoted ) {
			buff.insert( 0, openQuote );
			buff.append( closeQuote );
		}
		return buff.toString();
	}

	public String unquoted() {
		StringBuilder buff = new StringBuilder( seg3 );
		if ( seg2 != null ) {
			buff.insert( 0, seg2 + delimiter );
		}
		if ( seg1 != null ) {
			buff.insert( 0, seg1 + delimiter );
		}
		return buff.toString();
	}

	public String canonicalName() {
		return quoted ? unquoted() : unquoted().toLowerCase();
	}
	
	public boolean isQuoted() {
		// For now, the only usage we're concerned about is if the entire
		// identifier is quoted.
		return quoted;
	}
	
	private String formatSeg(String seg, boolean segQuoted) {
		if ( seg == null ) return null;
		return segQuoted ? DEFAULT_QUOTE + seg + DEFAULT_QUOTE : seg;
	}
	
	protected String formatSeg(String seg, boolean segQuoted, Dialect dialect) {
		if ( seg == null ) return null;
		return segQuoted ? dialect.openQuote() + seg + dialect.closeQuote() : seg;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null ) {
			return false;
		}

		ObjectName that;

		if ( o instanceof ObjectName ) {
			that = (ObjectName) o;
		}
		else if ( o instanceof String ) {
			that = new ObjectName( (String) o );
		}
		else {
			return false;
		}

		return areEqual( seg1, that.seg1, seg1Quoted )
				&& areEqual( seg2, that.seg2, seg2Quoted )
				&& areEqual( seg3, that.seg3, seg3Quoted );
	}

	private boolean areEqual(String one, String other, boolean posQuoted) {
		if ( one == null ) {
			return other == null;
		}
		else {
			// If the segment or entire identifier is quoted, must match exactly.
			return posQuoted || quoted ? one.equals( other ) : one.equalsIgnoreCase( other );
		}
	}

	@Override
	public int hashCode() {
		String seg3ForHash = seg3Quoted || quoted ? seg3 : seg3.toLowerCase();
		String seg2ForHash = ( seg2Quoted || quoted || seg2 == null ) ? seg2 : seg2.toLowerCase();
		String seg1ForHash = ( seg1Quoted || quoted || seg1 == null ) ? seg1 : seg1.toLowerCase();

		int tmpHashCode = seg3ForHash.hashCode();
		tmpHashCode = 31 * tmpHashCode + ( seg2ForHash != null ? seg2ForHash.hashCode() : 0 );
		return 31 * tmpHashCode + ( seg1ForHash != null ? seg1ForHash.hashCode() : 0 );
	}

	@Override
	public String toString() {
		return quoted();
	}
	
	/*
	 * The following exists solely for Table.
	 */
	public String getSeg1() {
		return seg1;
	}
	public String getSeg1Quoted( Dialect dialect ) {
		return formatSeg( seg1, seg1Quoted, dialect );
	}
	public String getSeg1Quoted() {
		return formatSeg( seg1, seg1Quoted );
	}
	public String getSeg2() {
		return seg2;
	}
	public String getSeg2Quoted() {
		return formatSeg( seg2, seg2Quoted );
	}
	public String getSeg2Quoted( Dialect dialect ) {
		return formatSeg( seg2, seg2Quoted, dialect );
	}
	public String getSeg3() {
		return seg3;
	}
	public String getSeg3Quoted() {
		return formatSeg( seg3, seg3Quoted );
	}
	public String getSeg3Quoted( Dialect dialect ) {
		return formatSeg( seg3, seg3Quoted, dialect );
	}
	public void setSeg1(String seg1) {
		seg1Quoted = StringHelper.isQuoted( seg1 );
		this.seg1 = StringHelper.unquote( seg1 );
	}
	public void setSeg2(String seg2) {
		seg2Quoted = StringHelper.isQuoted( seg2 );
		this.seg2 = StringHelper.unquote( seg2 );
	}
	public void setSeg3(String seg3) {
		seg3Quoted = StringHelper.isQuoted( seg3 );
		this.seg3 = StringHelper.unquote( seg3 );
	}
}
