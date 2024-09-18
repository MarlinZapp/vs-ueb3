package paxos;

import org.oxoo2a.sim4da.Simulator;

public class Simulation {
    public static int NUMBER_OF_FISHES = 20;
    public static double DANGER_CHANCE = 0.01;
    public static double FOOD_OCCURENCE = 0.05;

    public static boolean verbose = false;

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        Swarm swarm = new Swarm(NUMBER_OF_FISHES);
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
