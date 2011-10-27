package net.praqma.jenkins.plugin.ava;

import java.io.File;
import java.io.PrintStream;
import java.util.logging.Logger;

import net.praqma.util.debug.appenders.StreamAppender;
import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.util.ClearCaseUCM;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.implementation.MercurialConfiguration;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

public class AVABuilder extends Builder {

	private AbstractConfiguration source;
	private AbstractConfiguration target;
	
	private boolean processingAll;
	
	private File sourcePath;
	private File targetPath;
	
	private String sourceBranch;
	private String targetBranch;
	
	private static StreamAppender app = new StreamAppender( System.out );
	
	static {
        app.setTemplate( "[%level]%space %message%newline" );
        net.praqma.util.debug.Logger.addAppender( app );
       	app.setMinimumLevel( net.praqma.util.debug.Logger.LogLevel.DEBUG );
	}

	@DataBoundConstructor
	public AVABuilder( /* Source */ String sourcePath, String sourceBranch, 
			           /* Target */ String targetPath, String targetBranch, 
			           /* Common */ boolean processingAll ) {
		
		logger.warning( "Constructing" );
		
		this.processingAll = processingAll;
		
		/* Source */
		this.sourcePath = new File( sourcePath );
		this.sourceBranch = sourceBranch;
		
		/* Target */
		this.targetPath = new File( targetPath );
		this.targetBranch = targetBranch;
	}

	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException {
		PrintStream out = listener.getLogger();
		
		out.println( "[AVA] Setting up source, " + this.sourcePath  );
		
		out.println( "[AVA] Setting up target, " + this.targetPath  );
		
		return true;
	}
	
	/**** Getters for Jenkins UI ****/
	
	/**
	 * Get the path of the branch
	 * @param st
	 * @return
	 */
	public String getViewpath( String st ) {
		try {
			if( st.startsWith( "s" ) ) {
				/* Source */
				return this.sourcePath.toString();
			} else {
				/* Target */
				return this.targetPath.toString();
			}
		} catch( Exception e ) {
			return null;
		}
	}
	
	/**
	 * Get the branch name
	 * @param st
	 * @return
	 * @throws ElementNotCreatedException
	 * @throws ElementDoesNotExistException
	 */
	public String getBranch( String st ) throws ElementNotCreatedException, ElementDoesNotExistException {
		if( st.startsWith( "s" ) ) {
			/* Source */
			return this.sourceBranch;
		} else {
			/* Target */
			return this.targetBranch;
		}
	}
	
	public boolean isProcessingAll() {
		return processingAll;
	}
	
	public boolean isOfType( String st, String type ) {
		logger.fine( "ST=" + st + ". TYPE:" + type );
		if( st.startsWith( "s" ) ) {
			/* Source */
			if( type.equals( "hg" ) && source instanceof MercurialConfiguration ) {
				return true;
			}
		} else {
			/* Target */
			if( type.equals( "hg" ) && source instanceof MercurialConfiguration ) {
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
			
			if( type.equals( "ccfp" ) ) {
				try {
					config = ClearCaseUCM.getConfigurationFromView( path );
				} catch( Exception e ) {
					logger.severe( "Unable to make CC view: " + e.getMessage() );
					e.printStackTrace();
				}
			} else if( type.equals( "hg" ) ) {
				String branch = data.getString( "branch" );
				config = new MercurialConfiguration( path, branch );
			}
			
			return config;
		}

		@Override
		public Builder newInstance( StaplerRequest req, JSONObject data ) {
			System.out.println( data.toString( 2 ) );
			
			JSONObject source = data.getJSONObject( "source_branchType" );
			JSONObject target = data.getJSONObject( "target_branchType" );
			
			/* Source */
			String sview = null;
			try {
				sview = source.getString( "viewpath" );
			} catch( JSONException e ) {
				sview = "source";
			}
			
			String sbranch = null;
			try {
				sbranch = source.getString( "branch" );
			} catch( JSONException e ) {
				
			}
			
			/* Target */
			String tview = null;
			try {
				tview = target.getString( "viewpath" );
			} catch( JSONException e ) {
				tview = "target";
			}
			
			String tbranch = null;
			try {
				tbranch = target.getString( "branch" );
			} catch( JSONException e ) {
				
			}
			
			boolean all = true;
			try {
				all = data.getBoolean( "processingAll" );
			} catch( JSONException e ) {
				
			}
			
			return new AVABuilder( sview, sbranch, tview, tbranch, all );
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
