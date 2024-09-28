package paxos;

import org.oxoo2a.sim4da.Simulator;

public class Simulation {
    // After this time, the simulation will shutdown
    public static int SIMULATION_TIME_MILLIS = 5000;
    // What the swarm size will be
    public static int NUMBER_OF_FISHES = 20;

    // a fish will swim away from this direction
    public static double DANGER_CHANCE = 0.01;
    // a fish will swim in this direction
    public static double FOOD_OCCURENCE = 0.05;
    // each fish wants a new decision after this a random amount of milliseconds between these two values
    public static int NEW_DECISION_TIMER_MILLIS_MIN = 20;
    public static int NEW_DECISION_TIMER_MILLIS_MAX = 500;

    // logs each message received onto the console, mostly for debugging
    public static boolean verbose = false;

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        Swarm swarm = new Swarm(NUMBER_OF_FISHES);
        simulator.simulate();
        new Thread(() -> {
            try {
                Thread.sleep(SIMULATION_TIME_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            simulator.shutdown();
        }).start();
        ;
    }
}
