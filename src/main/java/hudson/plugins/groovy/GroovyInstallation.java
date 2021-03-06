package hudson.plugins.groovy;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;

import org.kohsuke.stapler.DataBoundConstructor;

public class GroovyInstallation extends ToolInstallation implements EnvironmentSpecific<GroovyInstallation>, NodeSpecific<GroovyInstallation> {

    @DataBoundConstructor
    public GroovyInstallation(String name, String home, List<? extends ToolProperty<?>> properties){
    	super(name,home,properties);
    }

    /**
     * Gets the executable path of this groovy installation on the given target system.
     */
    public @CheckForNull String getExecutable(VirtualChannel channel) throws IOException, InterruptedException {
        return channel.call(new GetExecutable(getHome()));
    }
    private static class GetExecutable extends MasterToSlaveCallable<String, IOException> {
        private final String home;
        GetExecutable(String home) {
            this.home = home;
        }
        @Override
        public String call() throws IOException {
            String execName = "groovy";
            String groovyHome = Util.replaceMacro(home, EnvVars.masterEnvVars);
            File binDir = new File(groovyHome, "bin/");
            if (File.separatorChar == '\\') {
                if (new File(binDir, execName + ".exe").exists()) {
                    execName += ".exe";
                } else {
                    execName += ".bat";
                }
            }
            File exe = new File(binDir, execName);
            if (exe.exists()) {
                return exe.getPath();
            }
            return null;
        }
    }



    public GroovyInstallation forEnvironment(EnvVars environment) {
        return new GroovyInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public GroovyInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new GroovyInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Symbol("groovy")
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<GroovyInstallation> {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return "Groovy";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new GroovyInstaller(null));
        }

        @Override
        public GroovyInstallation[] getInstallations() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null ) {
                throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
            }
            
            return jenkins.getDescriptorByType(Groovy.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(GroovyInstallation... installations) {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                jenkins.getDescriptorByType(Groovy.DescriptorImpl.class).setInstallations(installations);
            }
        }

    }

}
