package paxos;

import java.util.HashMap;
import java.util.Map;

import org.oxoo2a.sim4da.Message;

public class Learner extends Fish {

    // key : proposal number, value : number of acknowledged proposals with this number
    private Map<Integer, Integer> numberOfProposals = new HashMap<>();

    public Learner(Swarm swarm, String name) {
        super(swarm, name);
    }

    @Override
    public void engage() {
        super.engage();
        new Learning().start();
    }

    @Override
    protected void changeDirection(Direction direction) {
        super.changeDirection(direction);
        numberOfProposals = new HashMap<>();
    }

    private void handleAcceptAcknowledgement(Proposal proposal) {
        Integer num = numberOfProposals.get(proposal.getNumber());
        if (num == null) {
            numberOfProposals.put(proposal.getNumber(), 1);
        } else {
            num++;
            if (swarm.isMajority(num)) {
                Message m = new Message();
                m.addHeader("type", MessageType.LEARN.name());
                m.add("direction", proposal.getValue().name());
                for (String fish : swarm.getFishNames()) {
                    sendBlindly(m, fish);
                }
            } else {
                numberOfProposals.put(proposal.getNumber(), num);
            }
        }
    }

    private class Learning extends Thread {
        @Override
        public void run() {
            while (true) {
                Message m = receive();
                String type = m.getHeader().get("type");
                if (type == null) {
                    throw new MissingMessageArgumentException("Missing message type header.");
                } else if (type == MessageType.ACCEPT_ACK.name()) {
                    String dirName = m.getPayload().get("proposal-value");
                    int proposalNumber = m.queryInteger("proposal-number");
                    handleAcceptAcknowledgement(new Proposal(proposalNumber, Direction.valueOf(dirName)));
                }
            }
        }
    }
}
