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
    private AtomicInteger receivedProposals = new AtomicInteger(0);
    private AtomicInteger highestReceivedProposalNumber = new AtomicInteger(-1);

    // to prevent multiple executions of the same decision
    private AtomicInteger lastExecutedDecision = new AtomicInteger(-1);

    /* general fish stuff */

    protected Swarm swarm;
    protected String name;
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
        Behaviour behaviour = new Behaviour();
        behaviour.setDaemon(true);
        behaviour.start();
        Communication communication = new Communication();
        communication.setDaemon(true);
        communication.start();
    }

    private void sendPrepareRequest() {
        Message m = new Message();
        m.addHeader("decision-number", proposal.getDecisionNumber());
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

    private synchronized void handlePrepareRequest(int proposalNumber, String senderName, int decisionNumber) {
        int old = this.acceptThreshold.get();
        if (proposalNumber > old) {
            this.acceptThreshold.set(proposalNumber);
        }
        Message m = new Message();
        m.addHeader("decision-number", decisionNumber);
        m.addHeader("type", MessageType.PREPARE_ANSWER.name());
        m.addHeader("sender-name", name);
        if (this.acceptedProposal != null) {
            m.add("proposal-number", this.acceptedProposal.getNumber());
            m.add("proposal-value", this.acceptedProposal.getValue().name());
        }
        System.out
                .println(name + " promises to accept no proposals with numbers lower than " + proposalNumber + ".");
        sendBlindly(m, senderName);
    }

    private void handlePrepareResponse(int decisionNumber) {
        if (proposal == null) {
            return;
        }
        int received = receivedProposals.incrementAndGet();
        if (swarm.isMajority(received)) {
            System.out.println(name + " sends accept request for swimming " + proposal.getValue() + ".");
            sendAcceptRequest(decisionNumber);
        }
    }

    private void handlePrepareResponse(int proposalNumber, Direction proposalValue, int decisionNumber) {
        if (proposal == null) {
            return;
        }
        if (proposalNumber > highestReceivedProposalNumber.get()) {
            highestReceivedProposalNumber.set(proposalNumber);
            this.proposal.setValue(proposalValue);
            System.out.println(name + " may support proposal " + proposalNumber + " in swimming "
                    + proposalValue.name() + ".");
        }

        handlePrepareResponse(decisionNumber);
    }

    private void sendAcceptRequest(int decisionNumber) {
        Message m = new Message();
        m.addHeader("decision-number", decisionNumber);
        m.addHeader("type", MessageType.ACCEPT_REQUEST.name());
        m.addHeader("sender-name", name);
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
        if (acceptedProposal != null) {
            // accept only one proposal
            return;
        }
        System.out.println(name + " accepted proposal " + proposal.getNumber() + ".");
        acceptedProposal = proposal;
        Message m = new Message();
        m.addHeader("type", MessageType.ACCEPT_ACK.name());
        m.addHeader("sender-name", name);
        m.addHeader("decision-number", proposal.getDecisionNumber());
        m.add("proposal-number", proposal.getNumber());
        m.add("proposal-value", proposal.getValue().name());
        for (String learner : swarm.getLearnerNames()) {
            sendBlindly(m, learner);
        }
    }

    protected void changeDirection(Direction direction, int proposalNumber, int decisionNumber) {
        // to prevent multiple executions of one decision (can come from multiple learners)
        if (decisionNumber == lastExecutedDecision.get()) {
            return;
        } else {
            lastExecutedDecision.set(decisionNumber);
        }

        System.out.println("Decision " + decisionNumber + ": " + name + " is now swimming " + direction + ".");
        this.direction = direction;

        // reset swarm proposal counter
        swarm.proposal.set(0);
        // reset proposer values
        acceptThreshold = new AtomicInteger(0);
        acceptedProposal = null;
        // reset acceptor values
        proposal = null;
        receivedProposals = new AtomicInteger(0);
        highestReceivedProposalNumber = new AtomicInteger(-1);
    }

    private class Communication extends Thread {
        @Override
        public void run() {
            while (true) {
                Message m = receive();
                new Thread(() -> handleMessage(m)).start();
            }
        }

        private void handleMessage(Message m) {

            String type = m.getHeader().get("type");
            String senderName = m.queryHeader("sender-name");
            int decisionNumber = m.queryHeaderInteger("decision-number");

            if (type == null) {
                throw new MissingMessageArgumentException("Missing message type header.");
            } else if (type == MessageType.LEARN.name()) {
                String dirName = m.getPayload().get("direction");
                String proposalNumberValue = m.getPayload().get("proposal-number");
                Direction newDirection = Direction.valueOf(dirName);
                int proposalNumber = Integer.parseInt(proposalNumberValue);
                if (Simulation.verbose) {
                    System.out.println(name + " received LEARN message for proposal with number " + proposalNumber
                            + " and value " + dirName + " from " + senderName + ".");
                }
                changeDirection(newDirection, proposalNumber, decisionNumber);
            } else {
                if (decisionNumber < swarm.decisionNumber.get()) {
                    // message from old decision -> ignore
                    return;
                }

                if (type == MessageType.PREPARE_REQUEST.name()) {
                    int proposalNumber = m.queryInteger("proposal-number");
                    if (Simulation.verbose) {
                        System.out.println(name + " received PREPARE_REQUEST from " + senderName + ".");
                    }
                    handlePrepareRequest(proposalNumber, senderName, decisionNumber);
                } else if (type == MessageType.PREPARE_ANSWER.name()) {
                    String proposalValue = m.query("proposal-value");
                    if (proposalValue == null) {
                        if (Simulation.verbose) {
                            System.out
                                    .println(name
                                            + " received PREPARE_ANSWER without proposal value and number from "
                                            + senderName + ".");
                        }
                        handlePrepareResponse(decisionNumber);
                    } else {
                        int proposalNumber = m.queryInteger("proposal-number");
                        if (Simulation.verbose) {
                            System.out.println(name + " received PREPARE_ANSWER with proposal value "
                                    + proposalValue
                                    + " and proposal number " + proposalNumber + " from " + senderName + ".");
                        }
                        handlePrepareResponse(proposalNumber, Direction.valueOf(proposalValue), decisionNumber);
                    }
                } else if (type == MessageType.ACCEPT_REQUEST.name()) {
                    String dirName = m.getPayload().get("proposal-value");
                    int proposalNumber = m.queryInteger("proposal-number");
                    if (Simulation.verbose) {
                        System.out.println(
                                name + " received ACCEPT_REQUEST for proposal with number " + proposalNumber
                                        + " and value " + dirName + " from " + senderName + ".");
                    }
                    handleAcceptRequest(new Proposal(proposalNumber, Direction.valueOf(dirName), decisionNumber));
                }
            }
        }
    }

    private class Behaviour extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(random.nextInt(Simulation.NEW_DECISION_TIMER_MILLIS_MAX));
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
                        proposal = new Proposal(swarm.proposal.getAndIncrement(), newDirection,
                                swarm.decisionNumber.get());
                        System.out.println(name + " wants to swim " + newDirection + " (proposal number is "
                                + proposal.getNumber() + ").");
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
