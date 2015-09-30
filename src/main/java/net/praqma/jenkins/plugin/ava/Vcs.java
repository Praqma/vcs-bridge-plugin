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

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import jenkins.model.Jenkins;
import net.praqma.vcs.util.configuration.AbstractConfiguration;

/**
 *
 * @author Mads
 */
public abstract class Vcs implements ExtensionPoint, Describable<Vcs>, Serializable {

    public abstract AbstractConfiguration generateIntialConfiguration(File workspace, boolean input) throws IOException;
    
    /**
     * Is the source or the target blank? If the source is blank we can replay the target into the source to get a 
     * baseline repository
     * @param workspace
     * @return 
     */
    public abstract boolean isFromScratch(File workspace);
    
    public void registerExtensions() {
        
    }
    
    public abstract void generate() throws IOException;
    
    protected transient AbstractConfiguration activeConfiguration;
    
    @Override
    public Descriptor<Vcs> getDescriptor() {
        return (VcsDescriptor<?>) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
    
    public static DescriptorExtensionList<Vcs, VcsDescriptor<?>> getAll() {
        return Jenkins.getInstance().<Vcs, VcsDescriptor>getDescriptorList(Vcs.class);
    }

    @Override
    public String toString() {
        if(activeConfiguration != null) {
            return activeConfiguration.toString();
        }
        return "Null configuration";        
    }
    
}
