package net.praqma.jenkins.plugin.ava;

import hudson.FilePath;
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
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.util.Date;
import java.util.List;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.model.exceptions.UnableToCheckoutCommitException;

public class CommitReplayer implements FileCallable<Result> {
	
	private static final long serialVersionUID = -4591556492458099617L;
	
	private Vcs source;
	private Vcs target;
	
	private BuildListener listener;
	private File workspacePathName;
    public final String logFileName;
    public final String avaStateXml;
    
	public CommitReplayer(BuildListener listener, Vcs source, Vcs target, File workspacePathName, String logFileName, String avaStateXml ) {
		this.source = source;
		this.target = target;		
		this.workspacePathName = workspacePathName;
        this.logFileName = logFileName;
        this.avaStateXml = avaStateXml;
        this.listener = listener;
	}
    
    
    public AVABuildAction previousAction(AbstractBuild<?,?> build) {
        AbstractBuild<?,?> b = build.getPreviousBuild();
        while(b != null) {
            if(b.getAction(AVABuildAction.class) != null) {
                return b.getAction(AVABuildAction.class);
            }
            b = b.getPreviousBuild();
        }        
        return null;
    }

    @Override
	public Result invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
        AbstractConfiguration sourceConfig,targetConfig;
		PrintStream out = listener.getLogger();
        
		StreamAppender app = new StreamAppender( out );
		app.setTemplate( "[%level]%space %message%newline" );
		net.praqma.util.debug.Logger.addAppender( app );
        app.setMinimumLevel( net.praqma.util.debug.Logger.LogLevel.DEBUG );

        File logFile = new File( logFileName );
		FileAppender fa = new FileAppender( logFile  );
        out.println("[AVA] Log file location: "+logFile.getAbsolutePath());
		fa.setMinimumLevel( LogLevel.DEBUG );
		net.praqma.util.debug.Logger.addAppender( fa );
		
		/* TODO Somehow detect clearcase configuration */
		UCM.setContext( UCM.ContextType.CLEARTOOL );
		
        //Create a file containing state
		File p = new File( avaStateXml );		
		p.createNewFile();
        out.println( "AVA:XML: " + p.getAbsolutePath() );
		try {
			new AVA( new XMLStrategy( p ) );
		} catch( IllegalStateException e ) {
			/* Whoops, AVA already defined */
		}

        
        sourceConfig = source.generateIntialConfiguration(workspacePathName, true);
        targetConfig = target.generateIntialConfiguration(workspacePathName, false);
        
        target.registerExtensions();
        source.registerExtensions();                        
        
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
            replay(sourceConfig, result, targetConfig, out, cc);
        } catch (ElementException | UnableToReplayException | UnsupportedBranchException e) {          
            throw new ReplayException(e, result);       
		} finally {
			net.praqma.util.debug.Logger.removeAppender( fa );
		}
		  
		return result;
	}

    /**
     * 
     * @param sourceConfig
     * @param result
     * @param targetConfig
     * @param out
     * @param cc
     * @throws ElementDoesNotExistException
     * @throws UnableToReplayException
     * @throws UnableToCheckoutCommitException
     * @throws ElementNotCreatedException
     * @throws UnsupportedBranchException 
     */
    private void replay(AbstractConfiguration sourceConfig, Result result, AbstractConfiguration targetConfig, PrintStream out, CommitCounter cc) throws ElementDoesNotExistException, UnableToReplayException, UnableToCheckoutCommitException, ElementNotCreatedException, UnsupportedBranchException {
        AbstractBranch sourceBranch = sourceConfig.getBranch();
        result.sourceBranch = sourceBranch;
        AbstractReplay replay = targetConfig.getReplay();
        result.targetBranch = targetConfig.getBranch();
        out.println( "[AVA] Initializing cycle" );
        
        //Update the source beforehand
        sourceBranch.update();
        
        //Old (!!WRONG) We only update the date when ava was run...not the date of the commit on the source branch!
        
        
        Date now = AVA.getInstance().getLastCommitDate(sourceBranch);
        if(now == null) {
            out.println("[AVA] Replaying commits from scratch");
        } else {
            out.println( String.format( "[AVA] Replaying commits from date %s", now ) );
        }
        
        //Get the latest commit date. State is now very fragile..but correct.
        //Date now = previousAction(build) != null ? previousAction(build).getLastCommit().getCommitterDate() : null;
        
        //Get the commits to from souce to be replayed onto target
        List<? extends AbstractCommit> commits = sourceBranch.getCommits(false, now);

        for(AbstractCommit acm : commits) {
            acm.load();
            sourceBranch.checkoutCommit(acm);
            //Setup the replay based on the source
            replay.replay(acm);
            result.lastCommit = acm;
            result.commitCount = cc.getCommitCount();
        }
        
        AVA.getInstance().setLastCommitDate( sourceBranch, new Date() );
    }
}
