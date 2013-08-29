/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.hbm2ddl;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformationBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jboss.logging.Logger;

/**
 * A commandline tool to update a database schema. May also be called from inside an application.
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class SchemaUpdate {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SchemaUpdate.class.getName());

	private MetadataImplementor metadata;
    private Configuration configuration;
    
	private final ConnectionHelper connectionHelper;
	private final SqlStatementLogger sqlStatementLogger;
	private Dialect dialect;

	private final List<Exception> exceptions = new ArrayList<Exception>();

	private Formatter formatter;

	private boolean haltOnError = false;
	private String outputFile = null;
	private String delimiter;

	public SchemaUpdate(MetadataImplementor metadata, Connection connection){
		this.metadata = metadata;
		ServiceRegistry serviceRegistry = metadata.getServiceRegistry();
		if ( connection != null ) {
			this.connectionHelper = new SuppliedConnectionHelper( connection );
		}
		else {
			this.connectionHelper = new SuppliedConnectionProviderConnectionHelper(
					serviceRegistry.getService( ConnectionProvider.class )
			);
		}
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}
	
	public SchemaUpdate(MetadataImplementor metadata) {
		this( metadata, null );
	}

	@Deprecated
	public SchemaUpdate(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	@Deprecated
	public SchemaUpdate(Configuration configuration, Properties properties) throws HibernateException {
		this.configuration = configuration;
		this.dialect = Dialect.getDialect( properties );

		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( properties );
		this.connectionHelper = new ManagedProviderConnectionHelper( props );

		this.sqlStatementLogger = new SqlStatementLogger( false, true );
		this.formatter = FormatStyle.DDL.getFormatter();
	}

	@Deprecated
	public SchemaUpdate(ServiceRegistry serviceRegistry, Configuration cfg) throws HibernateException {
		this.configuration = cfg;

		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		this.dialect = jdbcServices.getDialect();
		this.connectionHelper = new SuppliedConnectionProviderConnectionHelper( jdbcServices.getConnectionProvider() );

		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().applySettings( properties ).build();
	}

	public static void main(String[] args) {
		try {
			final Configuration cfg = new Configuration();
			final StandardServiceRegistryImpl serviceRegistry = createServiceRegistry( cfg.getProperties() );
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

			boolean script = true;
			// If true then execute db updates, otherwise just generate and display updates
			boolean doUpdate = true;
			String propFile = null;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[i].startsWith( "--" ) ) {
					if ( args[i].equals( "--quiet" ) ) {
						script = false;
					}
					else if ( args[i].startsWith( "--properties=" ) ) {
						propFile = args[i].substring( 13 );
					}
					else if ( args[i].startsWith( "--config=" ) ) {
						cfg.configure( args[i].substring( 9 ) );
					}
					else if ( args[i].startsWith( "--text" ) ) {
						doUpdate = false;
					}
					else if ( args[i].startsWith( "--naming=" ) ) {
						cfg.setNamingStrategy(
								( NamingStrategy ) classLoaderService.classForName( args[i].substring( 9 ) ).newInstance()
						);
					}
				}
				else {
					cfg.addFile( args[i] );
				}

			}

			if ( propFile != null ) {
				Properties props = new Properties();
				props.putAll( cfg.getProperties() );
				props.load( new FileInputStream( propFile ) );
				cfg.setProperties( props );
			}

			try {
				new SchemaUpdate( serviceRegistry, cfg ).execute( script, doUpdate );
			}
			finally {
				serviceRegistry.destroy();
			}
		}
		catch ( Exception e ) {
            LOG.unableToRunSchemaUpdate(e);
			e.printStackTrace();
		}
	}

	/**
	 * Execute the schema updates
	 *
	 * @param script print all DDL to the console
	 */
	public void execute(boolean script, boolean doUpdate) {
		execute( Target.interpret( script, doUpdate ) );
	}
	
	// TODO: A lot of this needs to completely go away when 1.) .mapping/.cfg is replaced/repurposed and 2.)
	// SchemaManagementTool has an execution phase
	public void execute(Target target) {
        LOG.runningHbm2ddlSchemaUpdate();
        
		Statement stmt = null;
		Writer outputFileWriter = null;

        try {
        	connectionHelper.prepare( true );
    		Connection connection = connectionHelper.getConnection();
    		stmt = connection.createStatement();

    		exceptions.clear();
    		
    		List<SchemaUpdateScript> scripts;
        	
			if (metadata != null) {
				// metamodel

				// uses the schema management tool service to generate the scripts
				// longer term this class should instead just leverage the tool for its execution phase...
				
				ServiceRegistry serviceRegistry = metadata.getServiceRegistry();

				SchemaManagementTool schemaManagementTool = serviceRegistry.getService( SchemaManagementTool.class );
				final List<String> commands = new ArrayList<String>();
				final org.hibernate.tool.schema.spi.Target scriptTarget = new org.hibernate.tool.schema.spi.Target() {
					@Override
					public boolean acceptsImportScriptActions() {
						return false;
					}

					@Override
					public void prepare() {
						commands.clear();
					}

					@Override
					public void accept(String command) {
						commands.add( command );
					}

					@Override
					public void release() {
					}
				};
				
				final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcServices.class ).getJdbcEnvironment();
				final DatabaseInformation existingDatabase = new DatabaseInformationBuilder( jdbcEnvironment, connection )
						.prepareAll().build();

				final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
				schemaManagementTool.getSchemaMigrator( settings ).doMigration(
						metadata.getDatabase(), existingDatabase, false, scriptTarget );
				
				scripts = new ArrayList<SchemaUpdateScript>();
				for (String script : commands) {
					scripts.add( new SchemaUpdateScript( script, false ) );
				}
			}
			else {
				// configuration
				try {
	                LOG.fetchingDatabaseMetadata();
					DatabaseMetadata meta = new DatabaseMetadata( connection, dialect, configuration );
					scripts = configuration.generateSchemaUpdateScriptList( dialect, meta );
				}
				catch ( SQLException sqle ) {
					exceptions.add( sqle );
	                LOG.unableToGetDatabaseMetadata(sqle);
					throw sqle;
				}
			}

            LOG.updatingSchema();

			if ( outputFile != null ) {
                LOG.writingGeneratedSchemaToFile( outputFile );
				outputFileWriter = new FileWriter( outputFile );
			}

			for ( SchemaUpdateScript script : scripts ) {
				String formatted = formatter.format( script.getScript() );
				try {
					if ( delimiter != null ) {
						formatted += delimiter;
					}
					if ( target.doScript() ) {
						System.out.println( formatted );
					}
					if ( outputFile != null ) {
						outputFileWriter.write( formatted + "\n" );
					}
					if ( target.doExport() ) {
                        LOG.debug( script.getScript() );
						stmt.executeUpdate( formatted );
					}
				}
				catch ( SQLException e ) {
					if (!script.isQuiet()) {
						if ( haltOnError ) {
							throw new JDBCException( "Error during DDL export", e );
						}
						exceptions.add( e );
	                    LOG.unsuccessful(script.getScript());
	                    LOG.error(e.getMessage());
					}
				}
			}

            LOG.schemaUpdateComplete();

		}
		catch ( Exception e ) {
			exceptions.add( e );
            LOG.unableToCompleteSchemaUpdate(e);
		}
		finally {

			try {
				if ( stmt != null ) {
					stmt.close();
				}
				connectionHelper.release();
			}
			catch ( Exception e ) {
				exceptions.add( e );
                LOG.unableToCloseConnection(e);
			}
			try {
				if( outputFileWriter != null ) {
					outputFileWriter.close();
				}
			}
			catch(Exception e) {
				exceptions.add(e);
                LOG.unableToCloseConnection(e);
			}
		}
	}

	/**
	 * Returns a List of all Exceptions which occured during the export.
	 *
	 * @return A List containig the Exceptions occured during the export
	 */
	public List getExceptions() {
		return exceptions;
	}

	public void setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

	public void setFormat(boolean format) {
		this.formatter = ( format ? FormatStyle.DDL : FormatStyle.NONE ).getFormatter();
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
