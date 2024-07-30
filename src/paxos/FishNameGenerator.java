package paxos;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class FishNameGenerator {
    private static final String[] PREFIXES = { "An", "Be", "Cor", "Da", "El", "Fa", "Gi", "Ha", "In", "Jo" };
    private static final String[] MIDDLES = { "lan", "ri", "mo", "bel", "dor", "vin", "ra", "tan", "mi", "kel" };
    private static final String[] SUFFIXES = { "ar", "on", "ius", "el", "en", "ia", "or", "us", "is", "os" };

    private static final Random RANDOM = new Random();

    public static String generateRandomName() {
        StringBuilder sb = new StringBuilder("");
        sb.append(PREFIXES[RANDOM.nextInt(PREFIXES.length)]);
        int amoundMiddles = RANDOM.nextInt(5);
        for (int i = 0; i < amoundMiddles; i++) {
            sb.append(MIDDLES[RANDOM.nextInt(MIDDLES.length)]);
        }
        sb.append(SUFFIXES[RANDOM.nextInt(SUFFIXES.length)]);
        return sb.toString();
    }

    public static Set<String> generateUniqueNames(int count) {
        Set<String> uniqueNames = new HashSet<>();
        while (uniqueNames.size() < count) {
            uniqueNames.add(generateRandomName());
        }
        return uniqueNames;
    }
}
