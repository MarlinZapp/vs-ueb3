package paxos;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.UnknownNodeException;

public class Fish extends Node {
    private static Random random = new Random();

    /* acceptor values */

    // don't accept proposals with proposal number lower than this number
    private AtomicInteger acceptThreshold = new AtomicInteger(0);
    // accepted proposal with the highest proposal number
    private Proposal acceptedProposal;

    /* proposer values */

    // the proposal this proposer wants to propose
    private Proposal proposal;
    // the number of other proposals that have been received after the prepare call
    private AtomicInteger reveicedProposals = new AtomicInteger(0);
    private AtomicInteger highestReceivedProposalNumber = new AtomicInteger(-1);

    /* general fish stuff */

    protected Swarm swarm;
    private String name;
    private Direction direction;

    public Fish(Swarm swarm, String name) {
        super(name);
        this.swarm = swarm;
        this.name = name;
        this.direction = Direction.FRONT;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void engage() {
        new Behaviour().start();
        new Communication().start();
    }

    private void sendPrepareRequest() {
        Message m = new Message();
        m.addHeader("type", MessageType.PREPARE_REQUEST.name());
        m.addHeader("sender-name", name);
        m.add("proposal-number", proposal.getNumber());
        for (String fish : swarm.getFishNames()) {
            try {
                send(m, fish);
            } catch (UnknownNodeException e) {
                System.err.println("Simulator cannot find fish " + fish);
                e.printStackTrace();
            }
        }
    }

    private void handlePrepareRequest(int proposalNumber, String senderName) {
        this.acceptThreshold.set(proposalNumber);
        Message m = new Message();
        m.addHeader("type", MessageType.PREPARE_ANSWER.name());
        if (this.acceptedProposal != null) {
            m.add("proposal-number", this.acceptedProposal.getNumber());
            m.add("proposal-value", this.acceptedProposal.getValue().name());
        }
        sendBlindly(m, senderName);
    }

    private void handlePrepareResponse() {
        int received = reveicedProposals.incrementAndGet();
        if (swarm.isMajority(received)) {
            sendAcceptRequest();
        }
    }

    private void handlePrepareResponse(int proposalNumber, Direction proposalValue) {
        if (proposalNumber > highestReceivedProposalNumber.get()) {
            highestReceivedProposalNumber.set(proposalNumber);
            this.proposal.setValue(proposalValue);
        }
        handlePrepareResponse();
    }

    private void sendAcceptRequest() {
        Message m = new Message();
        m.addHeader("type", MessageType.ACCEPT_REQUEST.name());
        m.add("proposal-number", proposal.getNumber());
        m.add("proposal-value", proposal.getValue().name());
        for (String fish : swarm.getFishNames()) {
            try {
                send(m, fish);
            } catch (UnknownNodeException e) {
                System.err.println("Simulator cannot find fish " + fish);
                e.printStackTrace();
            }
        }
    }

    private void handleAcceptRequest(Proposal proposal) {
        if (proposal.getNumber() < this.acceptThreshold.get()) {
            // optional : send special message to sender that he is too late
            return;
        }
        acceptedProposal = proposal;
        Message m = new Message();
        m.addHeader("type", MessageType.ACCEPT_ACK.name());
        m.add("proposal-number", proposal.getNumber());
        m.add("proposal-value", proposal.getValue().name());
        for (String learner : swarm.getLearnerNames()) {
            sendBlindly(m, learner);
        }
    }

    protected void changeDirection(Direction direction) {
        // acceptedProposal == null means we have already entered this function since the accept
        // this function can still be called multiple times if the handleAcceptRequest() function is called again
        if (acceptedProposal == null) {
            return;
        }
        System.out.println(name + ": I'm now swimming to the " + direction);
        this.direction = direction;

        // reset swarm proposal counter
        swarm.proposal.set(0);
        // reset proposer values
        acceptThreshold = new AtomicInteger(0);
        acceptedProposal = null;
        // reset acceptor values
        proposal = null;
        reveicedProposals = new AtomicInteger(0);
        highestReceivedProposalNumber = new AtomicInteger(-1);
    }

    private class Communication extends Thread {
        @Override
        public void run() {
            while (true) {
                Message m = receive();
                String type = m.getHeader().get("type");
                // System.out.println(type);
                if (type == null) {
                    throw new MissingMessageArgumentException("Missing message type header.");
                } else if (type == MessageType.PREPARE_REQUEST.name()) {
                    String senderName = m.queryHeader("sender-name");
                    int proposalNumber = m.queryInteger("proposal-number");
                    handlePrepareRequest(proposalNumber, senderName);
                } else if (type == MessageType.PREPARE_ANSWER.name()) {
                    String proposalValue = m.query("proposal-value");
                    if (proposalValue == null) {
                        handlePrepareResponse();
                    } else {
                        int proposalNumber = m.queryInteger("proposal-number");
                        handlePrepareResponse(proposalNumber, Direction.valueOf(proposalValue));
                    }
                } else if (type == MessageType.ACCEPT_REQUEST.name()) {
                    String dirName = m.getPayload().get("proposal-value");
                    int proposalNumber = m.queryInteger("proposal-number");
                    handleAcceptRequest(new Proposal(proposalNumber, Direction.valueOf(dirName)));
                } else if (type == MessageType.LEARN.name()) {
                    String dirName = m.getPayload().get("direction");
                    Direction newDirection = Direction.valueOf(dirName);
                    changeDirection(newDirection);
                }
            }
        }
    }

    private class Behaviour extends Thread {
        @Override
        public void run() {
            while (true) {
                int secondsJustSwimming = random.nextInt(10);
                try {
                    Thread.sleep(secondsJustSwimming * 1000);
                } catch (InterruptedException e) {
                    System.out.println(name + " has been interrupted in stupidly swimming.");
                    e.printStackTrace();
                }
                Optional<Direction> change = changeDirection();
                if (change.isEmpty()) {
                    // swimming happily in current direction
                } else {
                    Direction newDirection = change.get();
                    // only start proposal if none is active
                    if (proposal == null && direction != newDirection) {
                        proposal = new Proposal(swarm.proposal.getAndIncrement(), newDirection);
                        System.out.println(name + " wants to swim " + newDirection);
                        sendPrepareRequest();
                    }
                }
            }
        }

        private Optional<Direction> changeDirection() {
            // danger
            if (random.nextDouble() < Simulation.DANGER_CHANCE) {
                int where = random.nextInt(4);
                return Optional.of(Direction.oppositeFromInt(where));
            }
            // food
            if (random.nextDouble() < Simulation.FOOD_OCCURENCE) {
                int where = random.nextInt(4);
                return Optional.of(Direction.fromInt(where));
            }
            return Optional.empty();
        }
    }
}
