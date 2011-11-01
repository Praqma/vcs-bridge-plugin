package net.praqma.jenkins.plugin.ava;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;
import net.praqma.vcs.util.configuration.implementation.ClearCaseConfiguration;
import net.praqma.vcs.util.configuration.implementation.MercurialConfiguration;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Future;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

public class AVABuilder extends Builder {

	private AbstractConfiguration source;
	private AbstractConfiguration target;

	private boolean processingAll;
	private boolean printDebug;

	@DataBoundConstructor
	public AVABuilder( AbstractConfiguration source, AbstractConfiguration target, boolean processingAll, boolean printDebug ) {

		logger.warning( "Constructing" );

		this.processingAll = processingAll;
		this.printDebug = printDebug;

		this.source = source;
		this.target = target;
	}

	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException {
		PrintStream out = listener.getLogger();
		
		String workspace = "";
		try {
			EnvVars env = build.getEnvironment( listener );
			
			if( env.containsKey( "CC_VIEWPATH" ) ) {
				workspace = env.get( "CC_VIEWPATH" );
			} else {
				workspace = env.get( "WORKSPACE" );
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		out.println( "[AVA] Workspace: " + workspace );
		out.println( "[AVA] debug is " + printDebug );
		
		Future<Boolean> fb = null;
		try {
			fb = build.getWorkspace().actAsync( new RemoteSetup( listener, source, target, workspace, printDebug ) );
			fb.get();
		} catch (IOException e) {
			out.println( "[AVA] Unable to perform: " + e.getMessage() );
			e.printStackTrace();
			return false;
		} catch (ExecutionException e) {
			out.println( "[AVA] Unable to execute: " + e.getMessage() );
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**** Getters for Jenkins UI ****/

	/**
	 * Get the path of the branch
	 * 
	 * @param st
	 * @return
	 */
	public String getViewpath( String st ) {
		try {
			if( st.startsWith( "s" ) ) {
				/* Source */
				return this.source.getPathName();
			} else {
				/* Target */
				return this.target.getPathName();
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the branch name
	 * 
	 * @param st
	 * @return
	 * @throws ElementNotCreatedException
	 * @throws ElementDoesNotExistException
	 */
	public String getBranch( String st ) throws ElementNotCreatedException, ElementDoesNotExistException {
		if( st.startsWith( "s" ) ) {
			/* Source */
			if( source instanceof MercurialConfiguration ) {
				return ( (MercurialConfiguration) this.source ).getBranchName();
			}
		} else {
			/* Target */
			if( target instanceof MercurialConfiguration ) {
				return ( (MercurialConfiguration) this.target ).getBranchName();
			}
		}
		return "";
	}

	public boolean isProcessingAll() {
		return processingAll;
	}
	
	public boolean isPrintDebug() {
		return printDebug;
	}

	public boolean isOfType( String st, String type ) {
		logger.fine( "ST=" + st + ". TYPE:" + type );
		if( st.startsWith( "s" ) ) {
			/* Source */
			if( type.equals( "hg" ) && source instanceof MercurialConfiguration ) {
				return true;
			} else if( type.equals( "ccfp" ) && source instanceof ClearCaseConfiguration ) {
				return true;
			}
			
		} else {
			/* Target */
			if( type.equals( "hg" ) && target instanceof MercurialConfiguration ) {
				return true;
			} else if( type.equals( "ccfp" ) && target instanceof ClearCaseConfiguration ) {
				return true;
			}
		}

		return false;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public DescriptorImpl() {
			load();
		}

		public String getDisplayName() {
			return "AVA Bridging";
		}

		private AbstractConfiguration getConfiguration( JSONObject data ) {
			AbstractConfiguration config = null;

			String type = data.getString( "value" );

			File path = new File( data.getString( "viewpath" ) );
			String pathName = data.getString( "viewpath" );

			/* ClearCase */
			if( type.equals( "ccfp" ) ) {
				try {
					config = new ClearCaseConfiguration( pathName, null, null, null, null, null );
				} catch (ConfigurationException e) {
					logger.severe( "Could not create ClearCase configuration" );
					e.printStackTrace();
				}

				/* Mercurial */
			} else if( type.equals( "hg" ) ) {
				String branch = data.getString( "branch" );
				config = new MercurialConfiguration( pathName, branch );
			}

			return config;
		}

		@Override
		public Builder newInstance( StaplerRequest req, JSONObject data ) {
			System.out.println( data.toString( 2 ) );

			JSONObject sourceData = data.getJSONObject( "source_branchType" );
			JSONObject targetData = data.getJSONObject( "target_branchType" );

			AbstractConfiguration source = getConfiguration( sourceData );
			AbstractConfiguration target = getConfiguration( targetData );

			boolean all = true;
			try {
				all = data.getBoolean( "processingAll" );
			} catch (JSONException e) {

			}
			
			boolean debug = true;
			try {
				debug = data.getBoolean( "printDebug" );
			} catch (JSONException e) {

			}

			return new AVABuilder( source, target, all, debug );
		}

		@Override
		public boolean configure( StaplerRequest req, JSONObject data ) {
			System.out.println( data.toString( 2 ) );
			
			return true;
		}

		public FormValidation doCheck( @QueryParameter String value ) {
			// Executable requires admin permission
			return FormValidation.validateExecutable( value );
		}

		@Override
		public boolean isApplicable( Class<? extends AbstractProject> arg0 ) {
			return true;
		}

	}

	private static final Logger logger = Logger.getLogger( AVABuilder.class.getName() );
}
