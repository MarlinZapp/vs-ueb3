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

    // don't accept proposals lower than this number
    private AtomicInteger proposalThreshold = new AtomicInteger(0);
    // highest accepted proposal number
    private AtomicInteger activeProposalNumber = new AtomicInteger(-1);
    // value (direction) of the proposal with the highest accepted proposal number
    private AtomicInteger activeProposalValue = new AtomicInteger(-1);

    /* proposer values */

    // the value (direction) that this proposer wants to propose
    private AtomicInteger proposalValue = new AtomicInteger(-1);
    // the number of other proposals that have been received after the prepare call
    private AtomicInteger reveicedProposals = new AtomicInteger(0);
    private AtomicInteger highestReceivedProposalNumber = new AtomicInteger(-1);

    /* general fish stuff */

    private Swarm swarm;
    private String name;
    private Direction direction;

    public Fish(Swarm swarm, String name) {
        super(name);
        this.swarm = swarm;
        this.name = name;
        this.direction = Direction.FRONT;
    }

    @Override
    public void engage() {
        new Behaviour().start();
        new Communication().start();
    }

    private void sendPrepareRequest() {
        Message m = new Message();
        m.addHeader("step", PaxosStep.PREPARE.name());
        m.addHeader("type", MessageType.REQUEST.name());
        m.addHeader("sender-name", name);
        m.add("proposal-number", swarm.proposal.getAndIncrement());
        // sending to the majority would be enough but let's send it to every other fish
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
        this.proposalThreshold.set(proposalNumber);
        Message m = new Message();
        m.addHeader("step", PaxosStep.PREPARE.name());
        m.addHeader("type", MessageType.ANSWER.name());
        int value = this.activeProposalValue.get();
        if (value != -1) {
            m.add("proposal-number", this.activeProposalNumber.get());
            m.add("proposal-value", value);
        }
        sendBlindly(m, senderName);
    }

    private void handlePrepareResponse() {
        int received = reveicedProposals.incrementAndGet();
        if (swarm.isMajority(received)) {
            sendAcceptRequest();
            // reset proposer values
            proposalValue.set(-1);
            reveicedProposals.set(0);
        }
    }

    private void handlePrepareResponse(int proposalNumber, Direction proposalValue) {
        if (proposalNumber > highestReceivedProposalNumber.get()) {
            highestReceivedProposalNumber.set(proposalNumber);
            this.proposalValue.set(proposalValue.asInt());
        }
        handlePrepareResponse();
    }

    private void sendAcceptRequest() {
        Direction direction = Direction.fromInt(proposalValue.get());
        Message m = new Message();
        m.addHeader("step", PaxosStep.ACCEPT.name());
        m.addHeader("type", MessageType.REQUEST.name());
        m.add("direction", direction.name());
        m.add("proposal-id", swarm.proposal.getAndIncrement());
        // sending to the majority would be enough but let's send it to every other fish
        for (String fish : swarm.getFishNames()) {
            try {
                send(m, fish);
            } catch (UnknownNodeException e) {
                System.err.println("Simulator cannot find fish " + fish);
                e.printStackTrace();
            }
        }
    }

    private void accept(MessageType type, Direction direction, int proposalNumber) {
        if (proposalNumber < this.proposalThreshold.get()) {
            // optional : send special message to sender that he is too late
            return;
        }
    }

    private class Communication extends Thread {
        @Override
        public void run() {
            while (true) {
                Message m = receive();
                String step = m.getHeader().get("step");
                if (step == PaxosStep.PREPARE.name()) {
                    String type = m.getHeader().get("type");
                    if (type == null) {
                        throw new MissingMessageArgumentException("Missing message type header.");
                    } else if (type == MessageType.REQUEST.name()) {
                        int proposalNumber = m.queryInteger("proposal-number");
                        String senderName = m.queryHeader("sender-name");
                        handlePrepareRequest(proposalNumber, senderName);
                    } else if (type == MessageType.ANSWER.name()) {
                        String proposalValue = m.query("proposal-value");
                        if (proposalValue == null) {
                            handlePrepareResponse();
                        } else {
                            int proposalNumber = m.queryInteger("proposal-number");
                            handlePrepareResponse(proposalNumber, Direction.valueOf(proposalValue));
                        }
                    }
                } else if (step == PaxosStep.ACCEPT.name()) {
                    String type = m.getHeader().get("type");
                    if (type == null) {
                        throw new MissingMessageArgumentException("Missing message type header.");
                    } else {
                        String dirName = m.getPayload().get("direction");
                        int proposalNumber = m.queryInteger("proposal-id");
                        accept(MessageType.valueOf(type), Direction.valueOf(dirName), proposalNumber);
                    }
                } else {
                    throw new MissingMessageArgumentException("Missing paxos step header.");
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
                    Direction direction = change.get();
                    // only start proposal if none is active
                    if (proposalValue.get() == -1) {
                        proposalValue.set(direction.asInt());
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

    public String getName() {
        return this.name;
    }
}
