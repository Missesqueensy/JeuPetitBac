package PetitBac;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

public class AgentArbitre extends Agent {

    @Override
    protected void setup() {
        System.out.println("Arbitre prêt.");

        addBehaviour(new Behaviour() {
            private boolean finished = false;

            @Override
            public void action() {
                // 1. Choisir une lettre
                char letter = 'A';

                // 2. Envoyer la lettre aux joueurs
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("Gamer1", AID.ISLOCALNAME));
                msg.addReceiver(new AID("Gamer2", AID.ISLOCALNAME));
                msg.setContent(String.valueOf(letter));
                send(msg);
                System.out.println("Arbitre a envoyé la lettre: " + letter);

                // 3. Recevoir les réponses (ici on lit 2 réponses)
                int responsesReceived = 0;
                while (responsesReceived < 2) {
                    ACLMessage response = receive();
                    if (response != null) {
                        System.out.println("Arbitre a reçu: " + response.getContent() +
                                           " de " + response.getSender().getLocalName());
                        responsesReceived++;
                    } else {
                        block();
                    }
                }

                finished = true;
            }

            @Override
            public boolean done() {
                return finished;
            }
        });
    }
}
