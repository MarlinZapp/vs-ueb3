package paxos;

import java.util.Optional;
import java.util.Random;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.UnknownNodeException;

public class Fish extends Node {
    private static Random random = new Random();

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

    private void send(Direction direction, PaxosStep step) {
        Message m = new Message();
        m.addHeader("step", step.name());
        m.addHeader("type", MessageType.REQUEST.name());
        m.add("direction", direction.name());
        for (String fish : swarm.getNames()) {
            try {
                send(m, fish);
            } catch (UnknownNodeException e) {
                System.err.println("Simulator cannot find fish " + fish);
                e.printStackTrace();
            }
        }
    }

    private void prepare(MessageType type, Direction direction) {
        switch (type) {
        case REQUEST: {

        }
        }
    }

    private void accept(MessageType type, Direction direction) {
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
                    } else {
                        String dirName = m.getPayload().get("direction");
                        prepare(MessageType.valueOf(type), Direction.valueOf(dirName));
                    }
                } else if (step == PaxosStep.ACCEPT.name()) {
                    String type = m.getHeader().get("type");
                    if (type == null) {
                        throw new MissingMessageArgumentException("Missing message type header.");
                    } else {
                        String dirName = m.getPayload().get("direction");
                        accept(MessageType.valueOf(type), Direction.valueOf(dirName));
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
