package net.praqma.jenkins.plugin.ava;

import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import hudson.model.Action;

public class AVABuildAction implements Action {

	private AbstractConfiguration sourceConfiguration;
	private AbstractBranch sourceBranch;
	
	private AbstractConfiguration targetConfiguration;
	private AbstractBranch targetBranch;
	
	private int commitCount = 0;
	
	public AVABuildAction( int count ) {
		this.commitCount = count;
	}
	
	public int getCommitCount() {
		return commitCount;
	}
	
	public AbstractConfiguration getSourceConfiguration() {
		return sourceConfiguration;
	}

	public void setSourceConfiguration( AbstractConfiguration sourceConfiguration ) {
		this.sourceConfiguration = sourceConfiguration;
	}

	public AbstractBranch getSourceBranch() {
		return sourceBranch;
	}

	public void setSourceBranch( AbstractBranch sourceBranch ) {
		this.sourceBranch = sourceBranch;
	}

	public AbstractConfiguration getTargetConfiguration() {
		return targetConfiguration;
	}

	public void setTargetConfiguration( AbstractConfiguration targetConfiguration ) {
		this.targetConfiguration = targetConfiguration;
	}

	public AbstractBranch getTargetBranch() {
		return targetBranch;
	}

	public void setTargetBranch( AbstractBranch targetBranch ) {
		this.targetBranch = targetBranch;
	}

	public AVABuildAction( AbstractConfiguration sourceConfiguration, AbstractBranch sourceBranch, AbstractConfiguration targetConfiguration, AbstractBranch targetBranch ) {
		this.sourceConfiguration = sourceConfiguration;
		this.sourceBranch = sourceBranch;
		this.targetConfiguration = targetConfiguration;
		this.targetBranch = targetBranch;
	}
	
	public String getIconFileName() {
		return "graph.gif";
	}

	public String getDisplayName() {
		return "AVA Bridging";
	}

	public String getUrlName() {
		return "ava";
	}

}
