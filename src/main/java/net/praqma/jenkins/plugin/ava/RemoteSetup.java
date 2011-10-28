package net.praqma.jenkins.plugin.ava;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.util.debug.appenders.StreamAppender;
import net.praqma.vcs.util.ClearCaseUCM;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;
import net.praqma.vcs.util.configuration.implementation.ClearCaseConfiguration;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class RemoteSetup implements FileCallable<Boolean> {
	
	private static final long serialVersionUID = -4591556492458099617L;
	
	private AbstractConfiguration source;
	private AbstractConfiguration target;
	
	private BuildListener listener;
	private String workspacePathName;

	public RemoteSetup( BuildListener listener, AbstractConfiguration source, AbstractConfiguration target, String workspacePathName ) {
		this.source = source;
		this.target = target;
		
		this.listener = listener;
		this.workspacePathName = workspacePathName;
	}

	public Boolean invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
		PrintStream out = listener.getLogger();
		
		StreamAppender app = new StreamAppender( out );
		app.setTemplate( "[%level]%space %message%newline" );
		net.praqma.util.debug.Logger.addAppender( app );
		app.setMinimumLevel( net.praqma.util.debug.Logger.LogLevel.DEBUG );
		
		/* TODO Somehow detect clearcase configuration */
		UCM.setContext( UCM.ContextType.CLEARTOOL );
		
		out.println( "[AVA] Generating source on slave" );
		
		//try {
			source = checkConfiguration( source, workspacePathName );
			out.println( "[AVA] Checked" );
			//source.generate();
			out.println( "[AVA] Generated" );
			/*
		} catch (ConfigurationException e) {
			throw new IOException( "Could not generate source: " + e.getMessage() );
		}
		*/
		
		out.println( "[AVA] Generating target on slave" );
		
		//try {
			target = checkConfiguration( target, workspacePathName );
			//target.generate();
			/*
		} catch (ConfigurationException e) {
			throw new IOException( "Could not generate target: " + e.getMessage() );
		}*/
		
		return true;
	}
	
	private AbstractConfiguration checkConfiguration( AbstractConfiguration config, String workspacePathName ) throws IOException {
		/* ClearCase UCM */
		if( config instanceof ClearCaseConfiguration ) {
			ClearCaseConfiguration ccc = (ClearCaseConfiguration)config;
			/* Determine missing options */
			if( ccc.getPathName().length() == 0 ) {
				File path = new File( workspacePathName );
				if( path.exists()) {
					try {
						config = ClearCaseUCM.getConfigurationFromView( path );
					} catch ( Exception e ) {
						e.printStackTrace();
						throw new IOException( "Unable to get view from workspace: " + e.getMessage() );
					}
				} else {
					throw new IOException( "Could not determine path name of configuration" );
				}
			} else {
				File path = new File( config.getPathName() );
				try {
					config = ClearCaseUCM.getConfigurationFromView( path );
				} catch ( Exception e ) {
					e.printStackTrace();
					throw new IOException( "Unable to get view from path: " + e.getMessage() );
				}
			}
		}
		
		return config;
	}

}
