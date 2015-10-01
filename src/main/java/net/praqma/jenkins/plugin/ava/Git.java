/*
 * The MIT License
 *
 * Copyright 2015 Mads.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.praqma.jenkins.plugin.ava;

import hudson.Extension;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import net.praqma.vcs.AVA;
import net.praqma.vcs.model.extensions.GitPublisherListener;
import net.praqma.vcs.util.CommandLine;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;
import net.praqma.vcs.util.configuration.implementation.GitConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
/**
 *
 * @author Mads
 */
public class Git extends Vcs implements Input,Output {
    
    private static final Logger log = Logger.getLogger(Git.class.getName());
    private String branch,path,remote,url;
    
    //Default empty constructor for Autodetction
    public Git() { }
    
    @DataBoundConstructor
    public Git(String branch, String path) {
        this.branch = branch;
        this.path = path;
    }

    @Override
    public AbstractConfiguration generateIntialConfiguration(File ws, boolean input) throws IOException {
        File finalPath = StringUtils.isBlank(path) ? ws : new File(path);
        String selectedBranch = StringUtils.isBlank(branch) ? "master" : branch; 
        GitConfiguration mc;
        if(StringUtils.isBlank(url) || StringUtils.isBlank(remote)) {
            log.fine("Remote and/or location not specified. Defaulting to behaviour where commits are not pushed");
            mc = new GitConfiguration(finalPath, selectedBranch);
            mc.setPathName(finalPath.getAbsolutePath());
        } else {
            log.fine("Remote and/or location specfied. Push on creation turned on");
            mc = new GitConfiguration(finalPath, selectedBranch, url, remote);
            mc.setPathName(finalPath.getAbsolutePath());
        }
        this.activeConfiguration = mc;
        log.fine("Generation of intial configuration was a success");
        return mc;
    }

    @Override
    public void generate() throws IOException {
        try {
            log.fine("Generating the final configuration");
            activeConfiguration.generate();
        } catch (ConfigurationException ex) {
            throw new IOException("Generating for Git failed", ex);
        }
    }

    @Override
    public void registerExtensions() {
        AVA a = AVA.getInstance();
        if(a!=null) {            
            GitPublisherListener publisher = new GitPublisherListener(getRemote(), getBranch(), activeConfiguration.getPath());
            a.registerExtension("GitPublisher", publisher);
            log.fine("GitPublisher listener registered");            
        } else {
            log.fine("GitPublisher listener not registered. Ava instance not present.");
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
        File finalPath = StringUtils.isBlank(path) ? workspace : new File(path);
        File dotGit = new File(finalPath, ".git");
        if(!dotGit.exists()) {       
            CommandLine.run("git init", finalPath);           
            return true;
        } 
        return false;
    }

    /**
     * @return the remote
     */
    public String getRemote() {
        return remote;
    }

    /**
     * @param remote the remote to set
     */
    @DataBoundSetter
    public void setRemote(String remote) {
        this.remote = remote;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }


    @Extension
    public static final class GitDescriptor extends VcsDescriptor<Git> {

        @Override
        public String getDisplayName() {
            return "Git";
        }
        
    }
    
}
