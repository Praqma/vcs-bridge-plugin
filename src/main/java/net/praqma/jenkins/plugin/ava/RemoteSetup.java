package net.praqma.jenkins.plugin.ava;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.FileAppender;
import net.praqma.util.debug.appenders.StreamAppender;
import net.praqma.vcs.AVA;
import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.model.AbstractReplay;
import net.praqma.vcs.model.exceptions.UnableToReplayException;
import net.praqma.vcs.model.exceptions.UnsupportedBranchException;
import net.praqma.vcs.model.extensions.CommitCounter;
import net.praqma.vcs.persistence.XMLStrategy;
import net.praqma.vcs.util.Cycle;
import net.praqma.vcs.util.configuration.AbstractConfiguration;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import net.praqma.vcs.model.exceptions.ElementException;

public class RemoteSetup implements FileCallable<Result> {
	
	private static final long serialVersionUID = -4591556492458099617L;
	
	private Vcs source;
	private Vcs target;
	
	private BuildListener listener;
	private File workspacePathName;
	
	private boolean printDebug;
	
	public RemoteSetup( BuildListener listener, Vcs source, Vcs target, File workspacePathName, boolean printDebug ) {
		this.source = source;
		this.target = target;		
		this.listener = listener;
		this.workspacePathName = workspacePathName;
		
		this.printDebug = printDebug;
	}

    @Override
	public Result invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
        
        AbstractConfiguration sourceConfig,targetConfig;
		PrintStream out = listener.getLogger();
		
		StreamAppender app = new StreamAppender( out );
		app.setTemplate( "[%level]%space %message%newline" );
		net.praqma.util.debug.Logger.addAppender( app );
		if( printDebug ) {
			app.setMinimumLevel( net.praqma.util.debug.Logger.LogLevel.DEBUG );
		} else {
			app.setMinimumLevel( net.praqma.util.debug.Logger.LogLevel.INFO );
		}
		
        File logFile = new File( "ava-bridge.log" );
		FileAppender fa = new FileAppender( logFile  );
        out.println("[AVA] Log file location: "+logFile.getAbsolutePath());
		fa.setMinimumLevel( LogLevel.DEBUG );
		net.praqma.util.debug.Logger.addAppender( fa );
		
		/* TODO Somehow detect clearcase configuration */
		UCM.setContext( UCM.ContextType.CLEARTOOL );
		
		File p = new File( "ava.xml" );
		out.println( "AVA:XML: " + p.getAbsolutePath() );
		p.createNewFile();
		try {
			new AVA( new XMLStrategy( p ) );
		} catch( IllegalStateException e ) {
			/* Whoops, AVA already defined */
		}
		
        sourceConfig = source.generateIntialConfiguration(workspacePathName, false);
        targetConfig = target.generateIntialConfiguration(workspacePathName, true);
        
		Result result = new Result();
		result.sourceConfiguration = sourceConfig;
		result.targetConfiguration = targetConfig;
		
		CommitCounter cc = new CommitCounter();
		AVA.getInstance().registerExtension( "counter", cc );
        
        out.println( "[AVA] Generating source branch" );        
        source.generate();			
        out.println( "[AVA] Generating target branch" );
        target.generate();

        out.println( "[AVA] Source configuration: " + source.toString() );
        out.println( "[AVA] Target configuration: " + target.toString() );
		try {	
            AbstractBranch sourceBranch = source.activeConfiguration.getBranch();
            result.sourceBranch = sourceBranch;

            AbstractReplay replay = target.activeConfiguration.getReplay();
            result.targetBranch = target.activeConfiguration.getBranch();

            out.println( "[AVA] Initializing cycle" );
            Cycle.cycle( sourceBranch, replay, null );
        } catch (ElementException | UnableToReplayException | UnsupportedBranchException e) {          
            throw new IOException("Failed to replay on target", e);       
		} finally {
			net.praqma.util.debug.Logger.removeAppender( fa );
		}
		
		out.println( "[AVA] Created " + cc.getCommitCount() + " commit" + ( cc.getCommitCount() == 1 ? "" : "s" ) );
		result.commitCount = cc.getCommitCount();
		
		return result;
	}
}
