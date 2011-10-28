package net.praqma.jenkins.plugin.ava;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

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

	public RemoteSetup( BuildListener listener, AbstractConfiguration source, AbstractConfiguration target ) {
		this.source = source;
		this.target = target;
		
		this.listener = listener;
	}

	public Boolean invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
		PrintStream out = listener.getLogger();
		
		out.println( "Generating source on slave" );
		
		try {
			checkClearCaseConfiguration( source );
			source.generate();
		} catch (ConfigurationException e) {
			throw new IOException( "Could not generate source: " + e.getMessage() );
		}
		
		out.println( "Generating target on slave" );
		
		try {
			checkClearCaseConfiguration( target );
			target.generate();
		} catch (ConfigurationException e) {
			throw new IOException( "Could not generate target: " + e.getMessage() );
		}
		
		return null;
	}
	
	private void checkClearCaseConfiguration( AbstractConfiguration config ) throws IOException {
		if( config instanceof ClearCaseConfiguration ) {
			ClearCaseConfiguration ccc = (ClearCaseConfiguration)config;
			/* Determine missing options */
			if( ccc.getPathName().length() == 0 ) {
				throw new IOException( "Could not determine path name of configuration" );
			}
			
			/* So far, this is all we've got!!! */
		}
	}

}
