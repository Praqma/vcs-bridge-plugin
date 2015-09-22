package net.praqma.jenkins.plugin.ava;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;


import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Future;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AVABuilder extends Builder {

	private Vcs source;
	private Vcs target;

	private boolean processingAll;
	private boolean printDebug;

	@DataBoundConstructor
	public AVABuilder( Vcs source, Vcs target, boolean processingAll, boolean printDebug ) {
		this.processingAll = processingAll;
		this.printDebug = printDebug;
		this.source = source;
		this.target = target;
	}

    @Override
	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException, IOException {
		PrintStream out = listener.getLogger();
		
		File workspace = null;
        EnvVars env = build.getEnvironment( listener );

        if( env.containsKey( "CC_VIEWPATH" ) ) {
            workspace = new File(env.get( "CC_VIEWPATH" ));
        } else {
            workspace =  new File(env.get( "WORKSPACE" ));
        }

		out.println( "[AVA] Workspace: " + workspace.getAbsolutePath() );
		out.println( "[AVA]     Debug: " + printDebug );

		Result result = null;
		try {
			Future<Result> fb = build.getWorkspace().actAsync( new RemoteSetup( listener, source, target, workspace, printDebug ) );
			result = fb.get();
		} catch (IOException e) {
			out.println( "[AVA] Unable to perform: " + e.getMessage() );
            e.printStackTrace(out);
			return false;
		} catch (ExecutionException e) {
			out.println( "[AVA] Unable to execute: " + e.getMessage() );
			e.printStackTrace(out);
			return false;
		}
		
		AVABuildAction action = new AVABuildAction( result.commitCount );
		action.setSourceBranch( result.sourceBranch );
		action.setTargetBranch( result.targetBranch );
		build.getActions().add( action );		

		return true;
	}


	public boolean isProcessingAll() {
		return processingAll;
	}
	
	public boolean isPrintDebug() {
		return printDebug;
	}

    /**
     * @return the source
     */
    public Vcs getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(Vcs source) {
        this.source = source;
    }

    /**
     * @return the target
     */
    public Vcs getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(Vcs target) {
        this.target = target;
    }

    /**
     * @param processingAll the processingAll to set
     */
    public void setProcessingAll(boolean processingAll) {
        this.processingAll = processingAll;
    }

    /**
     * @param printDebug the printDebug to set
     */
    public void setPrintDebug(boolean printDebug) {
        this.printDebug = printDebug;
    }


	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public DescriptorImpl() {
			load();
		}

        @Override
		public String getDisplayName() {
			return "AVA Bridging";
		}        

		@Override
		public boolean isApplicable( Class<? extends AbstractProject> arg0 ) {
			return true;
		}
        
        public List<VcsDescriptor<?>> getInputs() {
            List<VcsDescriptor<?>> inputs = new ArrayList<>();
            for(VcsDescriptor<?> v : Vcs.getAll()) {                
                if(Input.class.isAssignableFrom(v.clazz)) {                   
                    inputs.add(v);
                }
            }
            return inputs;
        }
        
        public List<VcsDescriptor<?>> getOutputs() {
            List<VcsDescriptor<?>> outputs = new ArrayList<>();
            for(VcsDescriptor<?> v : Vcs.getAll()) {
                if(Output.class.isAssignableFrom(v.clazz)) {
                    outputs.add(v);
                }
            }            
            return outputs;
        }

	}
}
