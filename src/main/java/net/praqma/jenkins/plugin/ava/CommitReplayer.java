package net.praqma.jenkins.plugin.ava;

import java.io.File;
import java.io.IOException;
import net.praqma.vcs.AVA;
import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.model.AbstractReplay;
import net.praqma.vcs.model.exceptions.UnableToReplayException;
import net.praqma.vcs.model.exceptions.UnsupportedBranchException;
import net.praqma.vcs.model.extensions.CommitCounter;
import net.praqma.vcs.persistence.XMLStrategy;
import net.praqma.vcs.util.configuration.AbstractConfiguration;

import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.model.exceptions.UnableToCheckoutCommitException;

public class CommitReplayer implements FileCallable<Result> {
	
    private static final Logger log = Logger.getLogger(CommitReplayer.class.getName());
	
	private Vcs source;
	private Vcs target;
	
	private File workspacePathName;
    public final String logFileName;
    public final String avaStateXml;
 
	public CommitReplayer(Vcs source, Vcs target, File workspacePathName, String logFileName, String avaStateXml ) {
		this.source = source;
		this.target = target;		
		this.workspacePathName = workspacePathName;
        this.logFileName = logFileName;
        this.avaStateXml = avaStateXml;                
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
        
		File p = new File( avaStateXml );		
		p.createNewFile();
                
        XMLStrategy strategy = new XMLStrategy(p);
        AVA a = AVA.getInstance(strategy);
        
        sourceConfig = source.generateIntialConfiguration(workspacePathName, true);
        targetConfig = target.generateIntialConfiguration(workspacePathName, false);
        
        target.registerExtensions();
        source.registerExtensions();                        
        
		Result result = new Result();
		result.sourceConfiguration = sourceConfig;
		result.targetConfiguration = targetConfig;
		
		CommitCounter cc = new CommitCounter();
		AVA.getInstance(strategy).registerExtension( "counter", cc );
     
        source.generate();        
        target.generate();
         
		try {
            //Replay the source onto the target. Track state as commits are added.
            replay(a.getLastCommitDate(sourceConfig.getBranch()), sourceConfig, result, targetConfig, cc);
        } catch (ElementException | UnableToReplayException | UnsupportedBranchException e) {          
            //Throw the exception. The result (how much was actually migrated) is included in the exception
            throw new ReplayException(e, result);       
		}	
		return result;
	}
    
    //TODO: This is a bandaid solution
    private void removeReReplays(List<? extends AbstractCommit> source, List<? extends AbstractCommit> target) {
        
        Iterator<? extends AbstractCommit> it = source.iterator();
        while( it.hasNext()) {
            AbstractCommit acmS = it.next();
            for(AbstractCommit acmT : target) {
                if(acmT.getTitle().contains(acmS.getKey())) {
                    log.fine(String.format("Removed commit:%n%s%nWill not be replayed", acmS));
                    it.remove();
                    break;
                }
            }
        }
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
    private void replay(Date lastDate, AbstractConfiguration sourceConfig, Result result, AbstractConfiguration targetConfig, CommitCounter cc) throws ElementDoesNotExistException, UnableToReplayException, UnableToCheckoutCommitException, ElementNotCreatedException, UnsupportedBranchException {
        AbstractBranch sourceBranch = sourceConfig.getBranch();
        result.sourceBranch = sourceBranch;
        AbstractReplay replay = targetConfig.getReplay();
        result.targetBranch = targetConfig.getBranch();
        
        //Update the source beforehand
        sourceBranch.update();
        
        //Get the latest commit date. State is now very fragile..but correct.
        //Date now = previousAction(build) != null ? previousAction(build).getLastCommit().getCommitterDate() : null;

        //Get the commits to from souce to be replayed onto target
        
        //Branch
        //Git commits:      SHA (noload)
        //CCUCM baseline:   Baseline (noload)
        
        //Replay
        //CCUCM creates baselines with name = "AVA_" + commit.getKey(); //Key = FQN of baseline in Clearcase, SHA = in git
        //Git creates commits = commit.getTitle()
        
        List<? extends AbstractCommit> commits = sourceBranch.getCommits(true, lastDate);
        log.fine(String.format("[AVA] Found %s commits to bridge on source", commits.size()));        
        
        List<? extends AbstractCommit> commitsTarget = result.targetBranch.getCommits(true, lastDate);
        log.fine(String.format("[AVA] Found %s commits on target", commitsTarget.size()));
        
        //Since we always end up in situations where the Key from the source (baseline, sha) end up being part of the commit message on the target (title)
        //We can make extra sure we do not replay needlessly. 
        //Cycle.rotate(sourceBranch, commits, replay);
        removeReReplays(commits, commitsTarget);
        
        for(AbstractCommit acm : commits) {
            sourceBranch.checkoutCommit(acm);
            replay.replay(acm);
            result.lastCommit = acm;
            result.commitCount = cc.getCommitCount();
            log.fine( String.format("Replayed:%n%s", acm));
        }
    }
}
