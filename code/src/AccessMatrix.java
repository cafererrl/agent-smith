import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.abs;
import static java.lang.System.out;

public class AccessMatrix {
    enum Perm {         // Declare Permissions. Operational source of truth for Domains and Objects.
        NULL,           // 0 NULL
        RW,             // 1 READ-WRITE
        R,              // 2 READ
        W,              // 3 WRITE
        DoSwitch,       // 4 DOMAIN-SWITCH TRUE
        DoNotSwitch     // 5 DOMAIN-SWITCH FALSE
    }
    private static class File {
        int id;
        Semaphore semWriter;
        Semaphore lock;
        int readerCount; // Use int with lock (rather than AtomicInteger) to ensure that equality evaluations are protected
        String data;
    }
    private static ArrayList<File> files;
    private static Map<Integer, Map<Integer, Perm>> matrix;

    // Constructor
    private AccessMatrix(int numDomains, int numObjects, Map<Integer, Map<Integer, Perm>> matrix) throws InterruptedException {
        AccessMatrix.matrix = matrix;
        // Initialize files and semaphores guarding file objects data
        files = new ArrayList<File>(numObjects);
        for (int i=0; i < numObjects; i++) {
            File file = new File();
            file.id = i;
            // Use fair semaphores, to directly address a Writer starvation problem
            file.semWriter = new Semaphore(1, true);
            file.lock = new Semaphore(1, true);
            file.readerCount = 0;
            file.data = StringGenerator.generateString();
            files.add(file);
        }
        // Print object-file contents, useful for testing initial file string states
        out.println("\nMy Files:");
        for (int i=0; i < files.size(); i++) {
            out.println("File "+i+": "+files.get(i).data);
        }
        out.println();

        // Fork as many Threads as there are domains/users who will try to access the matrix data
        DomainThread[] domThreads = new DomainThread[numDomains];
        for (int i=0; i < numDomains; i++) {
            domThreads[i] = new DomainThread(i, numDomains, numObjects, matrix.get(i)); // Provide each thread with inner set of matrix
            domThreads[i].start();
        }
    }

    static boolean arbitrator(DomainThread requestingDomain, int entityId, Perm requestedOperation) {
        // Check which file permissions the requesting domain has against the requested operation
        Perm currPermHeld = matrix.get(requestingDomain.id).get(entityId);
        // Handle case where R/W permission held in matrix. Bitwise logic to avoid short-circuit evaluation.
        if (currPermHeld == Perm.RW & (requestedOperation == Perm.R | requestedOperation == Perm.W)) {
            return true;
        }
        return currPermHeld == requestedOperation;
    }

    static class DomainThread extends Thread {
        int id;
        int numDomains;
        int numObjects;
        Map<Integer, Perm> perms;

        // Constructor
        public DomainThread(int id, int numDomains, int numObjects, Map<Integer, Perm> perms) {
            this.id = id;
            this.numDomains = numDomains;
            this.numObjects = numObjects;
            this.perms = perms;
        }

        @Override
        public void run() {
            int[] tasks = getThreadOperations();
            for (int t: tasks) {
                switch (t) {
                    case 2: // READ
                        // Decide which file to READ to. Bound is exclusive, but we zero-index.
                        int randomReadFileId = ThreadLocalRandom.current().nextInt(0, numObjects);
                        out.println("DomainThread["+this.id + "] > Attempting to read File "+randomReadFileId+".");
                        attemptRead(randomReadFileId);
                        break;
                    case 3: // WRITE
                        // Decide which file to WRITE to. Bound is exclusive, but we zero-index.
                        int randomWriteFileId = ThreadLocalRandom.current().nextInt(0, numObjects);
                        out.println("DomainThread["+this.id + "] > Attempting to write to File "+randomWriteFileId+".");
                        attemptWrite(randomWriteFileId);
                        break;
                    case 4: // DOMAIN SWITCH
                        // Decide which domain to SWITCH to, but use generic entity id instead of domain id! Bound is exclusive, but we zero-index.
                        int randomSwitchEntityId = ThreadLocalRandom.current().nextInt(numObjects, numDomains+numObjects); // Domains only.
                        out.println("DomainThread["+this.id + "] > Attempting to SWITCH to Domain "+(randomSwitchEntityId-numObjects)+".");
                        attemptDomainSwitch(randomSwitchEntityId);
                        break;
                    default:
                        out.println(t + " <- Task case #. Something went wrong with fetching domain-thread requests...");
                }
            }
            out.println("DomainThread["+this.id + "] > Tasks complete.");
        }

        public int[] getThreadOperations() {
            int numOperations = ThreadLocalRandom.current().nextInt(5, 11); // Generate amount of 5-10 thread tasks
            int[] operations = new int [numOperations];
            for (int i=0; i < numOperations; i++) {
                operations[i] = ThreadLocalRandom.current().nextInt(2, 5); // Assign a read, write, or domain switch (Perm Rules 2-4) operation
            }
            return operations;
        }

        public void attemptDomainSwitch(int entityId) {
            // Check that this DomainThread can SWITCH to prescribed domain.
            if (arbitrator(this, entityId, Perm.DoSwitch)) {
                this.perms = matrix.get(entityId-numObjects);
                // out.println("THREAD"+this.id+" new perms: "+this.perms.values()); // test new perms
                out.println("DomainThread["+this.id + "] > SWITCH to Domain "+(entityId-numObjects)+".");
                idle();
            } else {
                out.println("DomainThread["+this.id + "] > Could not SWITCH to Domain "+(entityId-numObjects)+". Permission denied.");
                idle();
            }
        }

        public void attemptRead(int fileId) {
            File currFile = files.get(fileId);
            // Check that this DomainThread has PERM to READ to prescribed file.
            if (arbitrator(this, fileId, Perm.R)) {
                try { // Protect readerCount incrementation
                    currFile.lock.acquireUninterruptibly();
                    currFile.readerCount++;
                    if (currFile.readerCount == 1) { currFile.semWriter.acquireUninterruptibly(); }
                    currFile.lock.release();
                    // Read file
                    out.println("DomainThread["+this.id + "] > READ permission granted. File "+fileId+" contains: '"+files.get(fileId).data+"'.");
                    idle();
                    currFile.lock.acquireUninterruptibly();
                    currFile.readerCount--;
                    if (currFile.readerCount == 0) { currFile.semWriter.release(); }
                    currFile.lock.release();
                    throw new InterruptedException();
                } catch (InterruptedException ignored) {

                }
            } else {
                out.println("DomainThread["+this.id + "] > Could not READ File "+fileId+". Permission denied.");
                idle();
            }
        }

        public void attemptWrite(int fileId) {
            File currFile = files.get(fileId);
            // Check that this DomainThread has PERM tp WRITE to prescribed file.
            if (arbitrator(this, fileId, Perm.W)) {
                try {
                    currFile.semWriter.acquireUninterruptibly();
                    // Write to File
                    currFile.data = StringGenerator.generateString();
                    files.set(fileId, currFile);
                    out.println("DomainThread["+this.id + "] > WRITE permission granted. '"+files.get(fileId).data+"' written to File "+fileId+".");
                    idle();
                    currFile.semWriter.release();
                    throw new InterruptedException();
                } catch (InterruptedException ignored) {
                }
            } else {
                out.println("DomainThread["+this.id + "] > Could not WRITE to File "+fileId+". Permission denied.");
                idle();
            }
        }

        public void idle() {
            int amtCycles = ThreadLocalRandom.current().nextInt(3, 8);
            for (int i=0; i < amtCycles; i++) {
                Thread.yield();
            }
            out.println("DomainThread["+this.id + "] > Yielded "+amtCycles+" times.");
        }
    }



// Driver
    public static void main(String[] args) {
        // Generate random values for N Domains and M Objects respectively.
        Random rand = new Random(System.currentTimeMillis());
        int nDomains = (abs(rand.nextInt()) % 5) + 3;
        int mObjects = (abs(rand.nextInt()) % 5) + 3;
        int totalEntities = nDomains + mObjects;
        out.println("numDomains: " + nDomains + "\nnumObjects: " + mObjects);

        // Declare and initialize Matrix entries with RANDOM permissions.
        Map<Integer, Map<Integer, Perm>> matrix = new HashMap<Integer, Map<Integer, Perm>>();
        for (int domainKey=0; domainKey < nDomains; domainKey++) {
            matrix.put(domainKey, new HashMap<Integer, Perm>());
            for (int entityKey=0; entityKey < totalEntities; entityKey++) {
                if (entityKey < mObjects) {
                    // Generate only NULL/READWRITE/READ/WRITE perm on the file columns of the matrix (0-3)
                    int randZeroThruThree = (abs(new Random().nextInt()) % 4);
                    matrix.get(domainKey).put(entityKey, Perm.values()[randZeroThruThree]);
                } else {
                    if (domainKey == (entityKey-mObjects)) {
                        // Generate Domain Switch DENY Perm across Domain diagonal. Domains can't switch to the same Domain (5)
                        matrix.get(domainKey).put(entityKey, Perm.DoNotSwitch);
                    } else {
                        // Generate only either DSwitchAllow or DSwitchDeny Perm (4,5)
                        int randFourFive = (abs(new Random().nextInt()) % 2) + 4;
                        matrix.get(domainKey).put(entityKey, Perm.values()[randFourFive]);
                    }
                }
            }
        }

        // Pack up the matrix data for printing.
        // Build title row.
        ArrayList<String> titleRow = new ArrayList<>();
        titleRow.add("Object/Domain");
        for (int i=0; i < totalEntities; i++) {
            if (i < mObjects) {
                titleRow.add("FILE"+i);
            } else {
                titleRow.add("DOMAIN"+(i-mObjects));
            }
        }
        // Build row entries of matrix table data.
        ArrayList<ArrayList<String>> tableData = new ArrayList<>();
        tableData.add(titleRow);
        for (int i=0; i < nDomains; i++) {
            ArrayList<String> entryRow = new ArrayList<>();
            entryRow.add("DOMAIN"+i);
            for (int j=0; j < totalEntities; j++) {
                entryRow.add(matrix.get(i).get(j).toString());
            }
            tableData.add(entryRow);
        }
        String[][] arrTableData = tableData.stream().map(u -> u.toArray(new String[0])).toArray(String[][]::new);

        // Call Pretty Printer.
        final PrettyPrinter printer = new PrettyPrinter(out);
        printer.print(arrTableData);

        try {
            AccessMatrix am = new AccessMatrix(nDomains, mObjects, matrix);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}