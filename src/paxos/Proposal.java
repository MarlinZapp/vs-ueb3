package paxos;

import java.util.concurrent.atomic.AtomicInteger;

public class Proposal {

    private final AtomicInteger number;
    private volatile Direction value;

    public Proposal(int number, Direction value) {
        this.number = new AtomicInteger(number);
        this.value = value;
    }

    // Get the number value
    public int getNumber() {
        return number.get();
    }

    // Set the number value
    public void setNumber(int newNumber) {
        number.set(newNumber);
    }

    // Get the value attribute
    public Direction getValue() {
        return value;
    }

    // Set the value attribute
    public void setValue(Direction newValue) {
        value = newValue;
    }

    // Optionally, override toString() for a more readable output
    @Override
    public String toString() {
        return "Proposal{" +
                "number=" + number +
                ", value=" + value +
                '}';
    }
}