package net.praqma.jenkins.plugin.ava;

import hudson.AbortException;
import java.io.IOException;
import java.io.PrintStream;


import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AVABuilder extends Builder {

	private Vcs source;
	private Vcs target;

	@DataBoundConstructor
	public AVABuilder( Vcs source, Vcs target ) {
		this.source = source;
		this.target = target;
	}

    @Override
	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException, IOException {
		PrintStream out = listener.getLogger();
		EnvVars env = build.getEnvironment( listener );        
		File workspace = env.containsKey( "CC_VIEWPATH" ) ? new File(env.get( "CC_VIEWPATH" )) : new File(env.get( "WORKSPACE" )); 
        String avaStateFile = String.format( "ava-%s.xml", build.getProject().getDisplayName().replace(" ", "_") );
        String logFileName = String.format( "ava-%s-%s.log", build.getProject().getDisplayName().replace(" ", "_"), build.number );
        Result fb = null;
		try {
			fb = build.getWorkspace().act(new CommitReplayer(source, target, workspace, logFileName, avaStateFile )) ;            
            AVABuildAction action = new AVABuildAction( fb.commitCount );
            action.setSourceBranch( fb.sourceBranch );
            action.setTargetBranch( fb.targetBranch );
            build.getActions().add( action );
		} catch (ReplayException e) {
			out.println( "[AVA] Unable to perform: " + e.getMessage() );
            e.printStackTrace(out);
            fb = e.r;
            AVABuildAction action = new AVABuildAction( e.r.commitCount );
            action.setSourceBranch( e.r.sourceBranch );
            action.setTargetBranch( e.r.targetBranch );            
            build.getActions().add( action );
            throw new AbortException(e.getCause().getMessage());
		} finally {
            if(fb != null) {
                out.println( "[AVA] Created " + fb.commitCount + " commit" + ( fb.commitCount == 1 ? "" : "s" ) );            
            }
        } 

		return true;
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
