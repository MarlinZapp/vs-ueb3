package paxos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Swarm {

    private ArrayList<Fish> fishes;
    // subset of fishes that spread decisions
    private ArrayList<Learner> learners;
    public AtomicInteger proposal = new AtomicInteger(0);

    public Swarm(int howMuchIsTheFish) {
        fishes = new ArrayList<Fish>(howMuchIsTheFish);
        int amountLearners = howMuchIsTheFish / 10 + 1;
        learners = new ArrayList<Learner>(amountLearners);
        Set<String> names = FishNameGenerator.generateUniqueNames(howMuchIsTheFish);
        int i = 0;
        for (String name : names) {
            if (i++ < amountLearners) {
                Learner fish = new Learner(this, name);
                learners.add(fish);
                fishes.add(fish);
            } else {
                fishes.add(new Fish(this, name));
            }
        }
    }

    public boolean isMajority(int num) {
        return num > fishes.size();
    }

    public List<String> getLearnerNames() {
        return learners.stream().map(fish -> fish.getName()).collect(Collectors.toList());
    }

    public List<String> getFishNames() {
        return fishes.stream().map(fish -> fish.getName()).collect(Collectors.toList());
    }

    public int id(String fishName) {
        for (int i = 0; i < fishes.size(); i++) {
            if (fishes.get(i).getName() == fishName) {
                return i;
            }
        }
        return -1;
    }

}
