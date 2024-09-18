package paxos;

import java.util.HashMap;
import java.util.Map;

import org.oxoo2a.sim4da.Message;

public class Learner extends Fish {

    // key : proposal value, value : number of acknowledged proposals for this value
    private Map<Direction, Integer> numberOfProposals = new HashMap<>();

    public Learner(Swarm swarm, String name) {
        super(swarm, name);
    }

    @Override
    public void engage() {
        super.engage();
        new Learning().start();
        new Learning().start();
        new Learning().start();
        new Learning().start();
        new Learning().start();
    }

    @Override
    protected void changeDirection(Direction direction, int proposalNumber, int decisionNumber) {
        super.changeDirection(direction, proposalNumber, decisionNumber);
        numberOfProposals = new HashMap<>();
    }

    private void handleAcceptAcknowledgement(Proposal proposal) {
        Direction dir = proposal.getValue();
        synchronized (this) {
            Integer num = numberOfProposals.get(proposal.getValue());
            if (num == null) {
                numberOfProposals.put(dir, 1);
            } else {
                num++;
                numberOfProposals.put(dir, num);
            }
        }

        int num = numberOfProposals.get(dir);
        if (Simulation.verbose) {
            System.out.println("Learner " + name + " has " + num + " proposals for " + proposal.getValue() + ".");
        }
        if (swarm.isMajority(num)) {
            synchronized (this) {
                if (swarm.decisionNumber.get() == proposal.getDecisionNumber()) {
                    swarm.decisionNumber.incrementAndGet(); // if this learner is the first one that found a
                                                            // majority,
                                                            // increment the decision count
                }
            }
            System.out.println(
                    "Learner " + name + " reached a majority of proposals for " + proposal.getValue() + ".");
            Message m = new Message();
            m.addHeader("decision-number", proposal.getDecisionNumber());
            m.addHeader("type", MessageType.LEARN.name());
            m.addHeader("sender-name", name);
            m.add("direction", proposal.getValue().name());
            m.add("proposal-number", proposal.getNumber());
            for (String fish : swarm.getFishNames()) {
                sendBlindly(m, fish);
            }
        }
    }

    private void handleMessage(Message m) {
        String type = m.getHeader().get("type");
        String senderName = m.queryHeader("sender-name");
        if (type == null) {
            throw new MissingMessageArgumentException("Missing message type header.");
        } else if (type == MessageType.ACCEPT_ACK.name()) {
            int decisionNumber = m.queryHeaderInteger("decision-number");
            String dirName = m.getPayload().get("proposal-value");
            int proposalNumber = m.queryInteger("proposal-number");
            if (Simulation.verbose) {
                System.out.println(name + " received ACCEPT_ACK for proposal with number " + proposalNumber
                        + " and value " + dirName + " from "
                        + senderName + ".");
            }
            handleAcceptAcknowledgement(
                    new Proposal(proposalNumber, Direction.valueOf(dirName), decisionNumber));
        }
    }

    private class Learning extends Thread {
        @Override
        public void run() {
            while (true) {
                Message m = receive();
                new Thread(() -> handleMessage(m)).start();
            }
        }
    }
}
