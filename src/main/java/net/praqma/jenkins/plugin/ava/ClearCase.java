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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.vcs.model.exceptions.ElementException;
import net.praqma.vcs.util.ClearCaseUCM;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;
import net.praqma.vcs.util.configuration.implementation.ClearCaseConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Mads
 */
public class ClearCase extends Vcs implements Input,Output {

    private String customViewPath;
    
    @DataBoundConstructor
    public ClearCase() { }
        
    @Override
    public void generate() throws IOException {
        try {
            activeConfiguration.generate();
            ClearCaseConfiguration ccc = (ClearCaseConfiguration)activeConfiguration;            
            Baseline blFoundation =  ccc.getFoundationBaseline();
            Stream pStream = blFoundation.getStream();            
            ccc.setParentStream( pStream );
            activeConfiguration = ccc;
        } catch (ConfigurationException ex) {
            throw new IOException("Unable to generate configuration",ex);            
        }
    }

    @Override
    public AbstractConfiguration generateIntialConfiguration(File workspace, boolean input) throws IOException {
        try {
            if (StringUtils.isBlank(customViewPath)) {
                AbstractConfiguration acc = ClearCaseUCM.getConfigurationFromView(workspace, input);            
                ClearCaseConfiguration ccc = (ClearCaseConfiguration)acc;
                ccc.iDontCare();
                this.activeConfiguration = ccc;
            } else {
                this.activeConfiguration = ClearCaseUCM.getConfigurationFromView(new File(customViewPath), input);            
            }
            
            return this.activeConfiguration;
        } catch (ElementException | ConfigurationException ex) {
            Logger.getLogger(ClearCase.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException("Failed to generate configuration from ClearCase view", ex);
        }
        
    }

    /**
     * @return the customViewPath
     */
    public String getCustomViewPath() {
        return customViewPath;
    }

    /**
     * @param customViewPath the customViewPath to set
     */
    @DataBoundSetter
    public void setCustomViewPath(String customViewPath) {
        this.customViewPath = customViewPath;
    }

    @Override
    public boolean isFromScratch(File workspace) {
        return false;
    }
    
    @Extension
    public static final class ClearCaseDescriptor extends VcsDescriptor<ClearCase> {

        @Override
        public String getDisplayName() {
            return "ClearCase";
        }
    
    }
}
