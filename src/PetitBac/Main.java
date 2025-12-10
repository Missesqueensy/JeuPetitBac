package PetitBac;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Main {
    
    public static void main(String[] args) {
        try {
            // Créer le runtime JADE
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.GUI, "true"); // Interface graphique JADE
            
            // Créer le conteneur principal
            AgentContainer container = rt.createMainContainer(p);
            
            System.out.println("========================================");
            System.out.println("   PETIT BAC - Jeu Multi-Agents        ");
            System.out.println("========================================\n");
            
            // Créer l'arbitre
            AgentController arbitre = container.createNewAgent(
                "AgentArbitre", 
                "PetitBac.AgentArbitre", 
                new Object[]{}
            );
            arbitre.start();
            
            // Attendre un peu avant de créer les joueurs
            Thread.sleep(500);
            
            // Créer le Joueur 1 avec BFS
            AgentController joueur1 = container.createNewAgent(
                "AgentJoueur1", 
                "PetitBac.AgentGamer", 
                new Object[]{"BFS"}  // Utilise BFS
            );
            joueur1.start();
            
            // Créer le Joueur 2 avec A*
            AgentController joueur2 = container.createNewAgent(
                "AgentJoueur2", 
                "PetitBac.AgentGamer", 
                new Object[]{"ASTAR"}  // Utilise A*
            );
            joueur2.start();
            
            System.out.println("\n✅ Tous les agents sont créés !");
            System.out.println("   - AgentArbitre");
            System.out.println("   - AgentJoueur1 (algorithme: BFS)");
            System.out.println("   - AgentJoueur2 (algorithme: A*)");
            System.out.println("\nLe jeu va commencer...\n");
            
        } catch (StaleProxyException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}