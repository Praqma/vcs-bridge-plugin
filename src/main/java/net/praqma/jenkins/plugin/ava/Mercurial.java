package net.praqma.jenkins.plugin.ava;

import hudson.Extension;
import java.io.File;
import java.io.IOException;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;
import net.praqma.vcs.util.configuration.implementation.MercurialConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class Mercurial extends Vcs implements Input,Output {
    
    private String branch;
    private String path;
    
    @DataBoundConstructor
    public Mercurial(String branch, String path) {
        this.branch = branch;
        this.path = path;
    }

    @Override
    public AbstractConfiguration generateIntialConfiguration(File workspace, boolean input) throws IOException {        
        File finalPath = StringUtils.isBlank(path) ? workspace : new File(path);
        
		MercurialConfiguration mc = new MercurialConfiguration(finalPath, branch);
        mc.setPathName(finalPath.getAbsolutePath());
        
        this.activeConfiguration = mc;
        return mc;
    }

    @Override
    public void generate() throws IOException {
        try {
            activeConfiguration.generate();
        } catch (ConfigurationException ex) {
            throw new IOException("Unable to generate mercurial configuration", ex);
        }
    }

    /**
     * @return the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @param branch the branch to set
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean isFromScratch(File workspace) {
        return false;
    }

    @Extension
    public static final class MercurialDesciptor extends VcsDescriptor<Mercurial> {

        @Override
        public String getDisplayName() {
            return "Mercurial";
        }
        
    }
}
