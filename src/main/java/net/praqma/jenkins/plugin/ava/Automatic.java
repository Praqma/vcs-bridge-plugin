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
import net.praqma.vcs.VersionControlSystems;
import net.praqma.vcs.util.VCS;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;
import net.praqma.vcs.util.configuration.implementation.ClearCaseConfiguration;
import net.praqma.vcs.util.configuration.implementation.MercurialConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class Automatic extends Vcs implements Input {
    
    @DataBoundConstructor
    public Automatic () { }

    @Override
    public AbstractConfiguration generateIntialConfiguration(File wspace, boolean input) throws IOException {

        VersionControlSystems vcs = VCS.determineVCS( wspace );
        
        if(vcs.equals(VersionControlSystems.ClearCase)) {            
            this.activeConfiguration = new ClearCase().generateIntialConfiguration(wspace, input);    
            return this.activeConfiguration;            
        } else if (vcs.equals(VersionControlSystems.Mercurial)) {
            this.activeConfiguration =  new MercurialConfiguration(wspace, "default");
            return this.activeConfiguration;
        } else if (vcs.equals(VersionControlSystems.Git)) {
            return null;
        } else {
            return null;
        }
    }

    @Override
    public void generate() throws IOException {
        try {
            this.activeConfiguration.generate();
        } catch (ConfigurationException cex) {
            throw new IOException("Error in generation for Automatic", cex);
        }
    }

    @Extension
    public static final class AutomaticDescriptor extends VcsDescriptor<Automatic> {

        @Override
        public String getDisplayName() {
            return "Determine automatically";
        }
    
    }
    
}