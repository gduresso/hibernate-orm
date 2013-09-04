/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.schema.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Exportable;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Sequence;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.Target;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class SchemaMigratorImpl implements SchemaMigrator {
	
	@Override
	public void doMigration(
			Database database,
			DatabaseInformation existingDatabase,
			boolean createSchemas,
			List<Target> targets) throws SchemaManagementException {
		doMigration( database, existingDatabase, createSchemas, targets.toArray( new Target[ targets.size() ] ) );
	}
	
	@Override
	public void doMigration(
			Database database,
			DatabaseInformation existingDatabase,
			boolean createSchemas,
			Target... targets) throws SchemaManagementException {

		for ( Target target : targets ) {
			target.prepare();
		}

		doMigrationToTargets( database, existingDatabase, createSchemas, targets );

		for ( Target target : targets ) {
			target.release();
		}
	}


	protected void doMigrationToTargets(
			Database database,
			DatabaseInformation existingDatabase,
			boolean createSchemas,
			Target[] targets) {
		
		final Dialect dialect = database.getJdbcEnvironment().getDialect();

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		for ( Schema schema : database.getSchemas() ) {
			if ( createSchemas ) {
				// TODO : add dialect method for getting a CREATE SCHEMA command and use it here
			}

			for ( Table table : schema.getTables() ) {
				if( !table.isPhysicalTable() ){
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				final TableInformation tableInformation = existingDatabase.getTableInformation( table.getTableName() );
				if ( tableInformation == null ) {
					createTable( table, database.getJdbcEnvironment(), targets );
				}
				else {
					migrateTable( table, tableInformation, targets, database.getJdbcEnvironment() );
				}

				// TODO : handle org.hibernate.mapping.Table.sqlCommentStrings
			}

			for ( Table table : schema.getTables() ) {
				final TableInformation tableInformation = existingDatabase.getTableInformation( table.getTableName() );
				// TODO: Not correct if createTable was used above -- tableInformation still won't exist.
//				if ( tableInformation == null ) {
//					// big problem...
//					throw new SchemaManagementException( "BIG PROBLEM" );
//				}

				for ( Index index : table.getIndexes() ) {
					// TODO :
				}

				if ( !database.getJdbcEnvironment().getDialect().hasAlterTable() ) {
					continue;
				}
				
				UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy.interpret( properties
						.get( Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY ) );
				if (! constraintMethod.equals( UniqueConstraintSchemaUpdateStrategy.SKIP )) {
					for ( UniqueKey uniqueKey : table.getUniqueKeys() ) {
						// Skip if index already exists. Most of the time, this
						// won't work since most Dialects use Constraints. However,
						// keep it for the few that do use Indexes.
						if ( StringHelper.isNotEmpty( uniqueKey.getName() ) ) {
							final IndexInformation indexInformation = tableInformation.getIndex( 
									Identifier.toIdentifier( uniqueKey.getName() ) );
							if ( indexInformation != null ) {
								continue;
							}
						}
						String[] createStrings = uniqueKey.sqlCreateStrings( dialect );
						if ( createStrings.length > 0 )
							if ( constraintMethod.equals( UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY ) ) {
								String[] dropStrings = uniqueKey.sqlDropStrings( dialect );
								scripts.add( new SchemaUpdateScript( constraintDropString, true) );
							}
							scripts.add( new SchemaUpdateScript( constraintString, true) );
					}
				}

				for ( ForeignKey foreignKey : table.getForeignKeys() ) {
					final ForeignKeyInformation foreignKeyInformation = findMatchingForeignKey( foreignKey, tableInformation );
					// TODO : .. implement
				}
			}

			for ( Sequence sequence : schema.getSequences() ) {
				checkExportIdentifier( sequence, exportIdentifiers );
				final SequenceInformation sequenceInformation = existingDatabase.getSequenceInformation( sequence.getName() );
				if ( sequenceInformation != null ) {
					// nothing we really can do...
					continue;
				}

				applySqlStrings(
						database.getJdbcEnvironment().getDialect().getSequenceExporter().getSqlCreateStrings(
								sequence,
								database.getJdbcEnvironment()
						),
						targets
				);
			}
		}
	}

	private ForeignKeyInformation findMatchingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		throw new NotYetImplementedException();
	}

	private void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException(
					String.format(
							"Export identifier [%s] encountered more than once",
							exportIdentifier
					)
			);
		}
		exportIdentifiers.add( exportIdentifier );
	}

	private void createTable(Table table, JdbcEnvironment jdbcEnvironment, Target[] targets) {
		applySqlStrings(
				jdbcEnvironment.getDialect().getTableExporter().getSqlCreateStrings( table, jdbcEnvironment ),
				targets
		);
	}

	private static void applySqlStrings(String[] sqlStrings, Target[] targets) {
		if ( sqlStrings == null ) {
			return;
		}

		for ( Target target : targets ) {
			for ( String sqlString : sqlStrings ) {
				target.accept( sqlString );
			}
		}
	}


	protected void migrateTable(
			Table table,
			TableInformation tableInformation,
			Target[] targets,
			JdbcEnvironment jdbcEnvironment) {
		applySqlStrings(
				table.sqlAlterStrings( tableInformation, jdbcEnvironment ),
				targets
		);

	}

}
