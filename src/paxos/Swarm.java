package paxos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Swarm {

    private ArrayList<Fish> fishes;
    public AtomicInteger decision = new AtomicInteger(0);

    public Swarm(int howMuchIsTheFish) {
        fishes = new ArrayList<Fish>(howMuchIsTheFish);
        Set<String> names = FishNameGenerator.generateUniqueNames(howMuchIsTheFish);
        for (String name : names) {
            fishes.add(new Fish(this, name));
        }
    }

    public List<String> getNames() {
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
