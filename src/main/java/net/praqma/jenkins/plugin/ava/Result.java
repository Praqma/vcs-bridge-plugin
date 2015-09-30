package net.praqma.jenkins.plugin.ava;

import java.io.Serializable;

import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.util.configuration.AbstractConfiguration;

public class Result implements Serializable {
	private static final long serialVersionUID = 6899955229485962441L;
	public AbstractConfiguration sourceConfiguration;
	public AbstractConfiguration targetConfiguration;
	
	public AbstractBranch sourceBranch;
	public AbstractBranch targetBranch;
    
    public AbstractCommit lastCommit;
	
	public int commitCount = 0;
}
