# Run the application from commandline in unix-like system

1. Clone the repo.
2. Run the following two commands using java 21 in the root directory.
```
javac -cp lib/sim4da-v2.jar -d bin $(find src -name "*.java")
java -cp bin:lib/sim4da-v2.jar paxos.Simulation
```

# How to run the application using an IDE

1. Clone the repo.
2. Add the .jar-file from the lib folder to the classpath of your IDE.
3. Run the main function in Simulation.java using java 21.

