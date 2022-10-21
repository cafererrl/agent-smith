import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class DiningPhilosophers {
    static AtomicInteger arriveCnt = new AtomicInteger(0);
    static AtomicInteger leaveCnt;
    static Semaphore numMeals; // used as an Atomic Integer because I couldn't cast it to a regular integer when needed
    private static Semaphore[] chopsticks;

    // Constructor
    private DiningPhilosophers(int numPhilosophers, int numMeals) throws InterruptedException {
        chopsticks = new Semaphore[numPhilosophers];
        leaveCnt = new AtomicInteger(0);
        PhiloThread[] philoThreads = new PhiloThread[numPhilosophers];
        DiningPhilosophers.numMeals = new Semaphore(numMeals);
        /* Create start/end barriers to ensure all the philosophers are present before dining,
           and that they leave at the same time.
           Instantiate as many chopsticks and philosopher-threads as the party size.
         */
        Semaphore start_barrier = new Semaphore(0);
        Semaphore end_barrier = new Semaphore(0);
        for (int i = 0; i < numPhilosophers; i++) {
            chopsticks[i] = new Semaphore(1);
            philoThreads[i] = new PhiloThread(i, numPhilosophers, start_barrier, end_barrier);
            philoThreads[i].start();
        }
    }

    static class PhiloThread extends Thread { // NEEDS to be kept static for use of atomic integer
        int id;
        int numThreads;
        static Semaphore start_barrier;
        static Semaphore end_barrier;
        static Semaphore leftChopstick;
        static Semaphore rightChopstick;

        // Constructor
        public PhiloThread(int threadId, int numThreads, Semaphore start_barrier, Semaphore end_barrier) {
            id = threadId;
            this.numThreads = numThreads;
            PhiloThread.start_barrier = start_barrier;
            PhiloThread.end_barrier = end_barrier;
            // Set default chopstick assignment. Reassign last philo-thread to avoid deadlocking
            if (id == numThreads - 1) {
                leftChopstick = chopsticks[(id + 1) % numThreads];
                rightChopstick = chopsticks[id];
            } else {
                leftChopstick = chopsticks[id];
                rightChopstick = chopsticks[(id + 1) % numThreads];
            }
        }

        @Override
        public void run() {
            // ENTER THE DINER AND SEAT THEM
            System.out.println("> Philosopher " + id + " has arrived!");
            arriveCnt.getAndIncrement();
            // Last philosopher will release the barrier
            if (arriveCnt.get() == numThreads) {
                System.out.println("\n<========== All philosophers present and seated. Bon appétit! ==========>\n");
                start_barrier.release();
            }
            // All but last threads will get stuck here
            try {
                start_barrier.acquire();
            } catch (Exception E) {
                System.out.println(E);
            }
            start_barrier.release(); // Open the flood gate

            // GOOD FOOD GOOD MEAT GOOD GOD LET'S EAT.
            // try while true with try catch on numMEals.aquireuninterruptibly in order to BLOCK and then finally releasE?
            while (numMeals.availablePermits() > 0) {
                attemptEating();
            }

            System.out.println("Philosopher " + id + " is done at the table.");
            // Finished eating, so assemble philoThreads before leaving one by one.
            leaveCnt.getAndIncrement();
//            System.out.println(leaveCnt);
            if (leaveCnt.get() == numThreads) {
                System.out.println("\n<========== All philosophers ready to depart. ==========>\n");
                end_barrier.release();
            }
            // All but last threads will get stuck here
            try {
                end_barrier.acquire();
            } catch (Exception E) {
                System.out.println(E);
            }
            System.out.println("> Philosopher " + id + " has left.");
            end_barrier.release();
        }

        public void attemptEating() {
            try {
                leftChopstick.acquireUninterruptibly();
                System.out.println("Philosopher " + id + "'s left chopstick is available.");
                int amtCycles = ThreadLocalRandom.current().nextInt(3, 7);
                for (int i = 0; i < amtCycles; i++) {
                    Thread.yield();
                }
                try {
                    rightChopstick.acquireUninterruptibly();
                    System.out.println("Philosopher " + id + "'s right chopstick is available.");
                    if (numMeals.availablePermits() > 0) {
                        eat();
                        System.out.println("Philosopher " + id + " returns their left chopstick.");
                        leftChopstick.release();
                        System.out.println("Philosopher " + id + " returns their right chopstick.");
                        rightChopstick.release();
                        think();
                    } else {
                        leftChopstick.release();
                        rightChopstick.release();
                        return;
                    }
                    throw new InterruptedException();
                } catch (InterruptedException e) {
                    System.out.println("Philosopher " + id + "'s right chopstick is NOT available.");
                }
                throw new InterruptedException();
            } catch (InterruptedException e) {
                System.out.println("Philosopher " + id + "'s left chopstick is NOT available.");
            }
        }

        public void think() {
            System.out.println("Philosopher " + id + " is thinking. 'Hmm...'");
            int amtCycles = ThreadLocalRandom.current().nextInt(3, 7);
            for (int i = 0; i < amtCycles; i++) {
                Thread.yield();
            }
        }

        public void eat() throws InterruptedException {
            numMeals.acquire();
            System.out.println("Philosopher " + id + " is eating. Yum! ...MEALS LEFT: " + numMeals.availablePermits());
            int amtCycles = ThreadLocalRandom.current().nextInt(3, 7);
            for (int i = 0; i < amtCycles; i++) {
                Thread.yield();
            }
        }
    }
    public static void main(String[] args) {
        int numPhilosophers;
        int numMeals;

        // Get user input for number of philosophers and meals
        do {
            System.out.println("How many philosophers will be dining (integer from 2-10000000)?:");
            Scanner userIn = new Scanner(System.in);
            numPhilosophers = userIn.nextInt();  // Read user input
        } while (numPhilosophers > 10000000 || numPhilosophers <= 1);

        do {
            System.out.println("How many meals shall they eat (integer from 1-10000000)?:");
            Scanner userIn = new Scanner(System.in);
            numMeals = userIn.nextInt();  // Read user input
        } while (numMeals > 10000000 || numMeals <= 0);

        try {
            DiningPhilosophers dp = new DiningPhilosophers(numPhilosophers, numMeals);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}