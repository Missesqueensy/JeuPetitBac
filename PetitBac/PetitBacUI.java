package PetitBac;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PetitBacUI extends JFrame implements AgentManager.LogListener {
    private final AgentManager manager;
    private final JTextArea logArea = new JTextArea(15, 60);

    public PetitBacUI() {
        super("PetitBac - Interface");
        manager = new AgentManager(this);
        initUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(8,8));

        JPanel top = new JPanel();
        JButton startPlatformBtn = new JButton("Start Platform");
        JButton stopPlatformBtn = new JButton("Stop Platform");
        JButton startAgentsBtn = new JButton("Start Agents");
        JButton stopAgentsBtn = new JButton("Stop Agents");

        top.add(startPlatformBtn);
        top.add(stopPlatformBtn);
        top.add(startAgentsBtn);
        top.add(stopAgentsBtn);

        panel.add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        JScrollPane sp = new JScrollPane(logArea);
        panel.add(sp, BorderLayout.CENTER);

        // Button actions
        startPlatformBtn.addActionListener(e -> {
            try { manager.startPlatform(); }
            catch (Exception ex) { log("Error starting platform: " + ex.getMessage()); ex.printStackTrace(); }
        });

        stopPlatformBtn.addActionListener(e -> {
            try { manager.stopPlatform(); }
            catch (Exception ex) { log("Error stopping platform: " + ex.getMessage()); ex.printStackTrace(); }
        });

        startAgentsBtn.addActionListener(e -> {
            try {
                // Start players with distinct algorithm lists
                String[] algos1 = new String[] {"Random","BFS","DFS"};
                String[] algos2 = new String[] {"UCS","A*"};
                // Ensure mutual exclusivity: remove any overlapping algos from Joueur2
                java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(algos1));
                java.util.List<String> filtered2 = new java.util.ArrayList<>();
                for (String a2 : algos2) if (!set1.contains(a2)) filtered2.add(a2);
                if (filtered2.size() != algos2.length) {
                    log("Adjusted Joueur2 algorithms to avoid overlap: " + filtered2);
                }
                String[] algos2final = filtered2.toArray(new String[0]);
                manager.startAgent("Joueur1", "PetitBac.AgentJoueur", algos1);
                manager.startAgent("Joueur2", "PetitBac.AgentJoueur", algos2final);
                manager.startAgent("ArbitreAgent", "PetitBac.AgentArbitre");
            } catch (Exception ex) { log("Error starting agents: " + ex.getMessage()); ex.printStackTrace(); }
        });

        stopAgentsBtn.addActionListener(e -> {
            try { manager.stopAllAgents(); }
            catch (Exception ex) { log("Error stopping agents: " + ex.getMessage()); ex.printStackTrace(); }
        });

        getContentPane().add(panel);
    }

    @Override
    public void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PetitBacUI ui = new PetitBacUI();
            ui.setVisible(true);
        });
    }
}
