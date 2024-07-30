package paxos;

import org.oxoo2a.sim4da.Simulator;

public class Simulation {
    public static double DANGER_CHANCE = 0.01;
    public static double FOOD_OCCURENCE = 0.05;

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        Swarm swarm = new Swarm(20);
        simulator.simulate();
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            simulator.shutdown();
        }).start();
        ;
    }
}
