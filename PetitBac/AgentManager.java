package PetitBac;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AgentManager {
    private Runtime rt;
    private ContainerController mainContainer;
    private final Map<String, AgentController> agents = new HashMap<>();
    private final LogListener listener;

    public interface LogListener { void log(String s); }

    public AgentManager(LogListener listener) {
        this.listener = listener;
    }

    public void startPlatform() throws Exception {
        if (mainContainer != null) {
            log("Platform already started");
            return;
        }
        rt = Runtime.instance();
        ProfileImpl p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");
        mainContainer = rt.createMainContainer(p);
        log("Main JADE container started (GUI enabled).");
    }

    public void stopPlatform() throws Exception {
        if (mainContainer == null) {
            log("Platform not running");
            return;
        }
        // kill platform
        try {
            mainContainer.getPlatformController().kill();
            log("Platform stopped.");
        } finally {
            mainContainer = null;
            agents.clear();
        }
    }

    public void startAgent(String name, String className) throws Exception {
        startAgent(name, className, new String[]{});
    }

    public void startAgent(String name, String className, String[] args) throws Exception {
        if (mainContainer == null) throw new IllegalStateException("Start platform first");
        if (agents.containsKey(name)) { log(name + " already started"); return; }
        AgentController ac = mainContainer.createNewAgent(name, className, new Object[]{args});
        ac.start();
        agents.put(name, ac);
        log("Started agent: " + name + " (" + className + ")");
    }

    public void stopAgent(String name) throws Exception {
        AgentController ac = agents.remove(name);
        if (ac != null) {
            ac.kill();
            log("Stopped agent: " + name);
        } else {
            log("Agent not found: " + name);
        }
    }

    public void stopAllAgents() throws Exception {
        ArrayList<String> list = new ArrayList<>(agents.keySet());
        for (String n : list) stopAgent(n);
    }

    private void log(String s) {
        if (listener != null) listener.log(s);
        else System.out.println(s);
    }
}
