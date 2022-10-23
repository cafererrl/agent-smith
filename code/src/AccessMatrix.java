import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.abs;
import static java.lang.System.out;

public class AccessMatrix {
    enum Perm {     // Declare Permissions. Operational source of truth for Domains and Objects.
        NULL,           // 0 NULL
        RW,             // 1 READ-WRITE
        R,              // 2 READ
        W,              // 3 WRITE
        DoSwitch,       // 4 DOMAIN-SWITCH TRUE
        DoNotSwitch     // 5 DOMAIN-SWITCH FALSE
    }
    private static Map<Integer, Map<Semaphore, String>> files; // Files are represented as (id, lock state, string data).

    // Constructor
    private AccessMatrix(int numDomains, int numObjects, Map<Integer, Map<Integer, Perm>> matrix) throws InterruptedException {
        // Initialize semaphores guarding file objects, and their respective random strings as file data, in hashmap
        files = new HashMap<Integer, Map<Semaphore, String>>();
        for (int i=0; i < numObjects; i++) {
            Map<Semaphore, String> fileEntity = new HashMap<Semaphore, String>();
            fileEntity.put(new Semaphore(1), StringGenerator.generateString());
            files.put(i, fileEntity);
        }
        // Print object-file contents, useful for testing initial file string states
        out.println("\nMy Files:");
        for (int i=0; i < files.size(); i++) {
            out.println("File "+i+": "+files.get(i).values());
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
        boolean hasPerm = false;
        for (int i=0; i < requestingDomain.perms.entrySet().size(); i++) {
            if (i == entityId & requestingDomain.perms.get(i) == requestedOperation) {
                hasPerm=true;
            } else {
                hasPerm=false;
            }
        }
        return hasPerm;
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
                        // Decide which file to READ to. Bound is exclusive, but we zero-index
                        int randomReadFileId = ThreadLocalRandom.current().nextInt(0, numObjects);
                        out.println("DomainThread["+this.id + "] > Attempting to read File "+randomReadFileId);
                        attemptRead(randomReadFileId);
                        break;
                    case 3: // WRITE
                        // Decide which file to WRITE to. Bound is exclusive, but we zero-index
                        int randomWriteFileId = ThreadLocalRandom.current().nextInt(0, numObjects);
                        out.println("DomainThread["+this.id + "] > Attempting to write to File "+randomWriteFileId);
                        attemptWrite(randomWriteFileId);
                        break;
                    case 4: // DOMAIN SWITCH
                        // Decide which domain to SWITCH to, but we use generic element id instead of domain id! Bound is exclusive, but we zero-index.
                        int randomSwitchEntityId = ThreadLocalRandom.current().nextInt(numObjects, numDomains+numObjects); // domains only
                        out.println("DomainThread["+this.id + "] > Attempting to SWITCH to Domain"+randomSwitchEntityId);
                        attemptDomainSwitch(randomSwitchEntityId);
                        break;
                    default:
                        out.println(t + " <- Something went wrong with fetching domain-thread requests...");
                }
            }


        }

        public int[] getThreadOperations() {
            int numOperations = ThreadLocalRandom.current().nextInt(5, 11); // Generate amount of 5-10 thread tasks
            int[] operations = new int [numOperations];
            for (int i=0; i < numOperations; i++) {
                operations[i] = ThreadLocalRandom.current().nextInt(2, 5); // Assign a read, write, or domain switch (Perm Rules 2-4) operation
            }
            return operations;
        }

        public boolean attemptDomainSwitch(int entityId) {
            // Check that this DomainThread can SWITCH to prescribed domain
            if (arbitrator(this, entityId, Perm.DoSwitch)) {
                out.println("DomainThread["+this.id + "] > Switched to Domain"+entityId);
                idle();
                return true;
            } else {
                out.println("DomainThread["+this.id + "] > Could not switch to Domain"+entityId+". Permission denied.");
                idle();
                return false;
            }
        }

        public boolean attemptRead(int fileId) {
            // Check that this DomainThread can READ to prescribed file
            if (arbitrator(this, fileId, Perm.R)) {
                out.println("DomainThread["+this.id + "] > File"+fileId+" contains: "+files.get(fileId).values());
                idle();
                return true;
            } else {
                out.println("DomainThread["+this.id + "] > Could not read File"+fileId+". Permission denied.");
                idle();
                return false;
            }
        }

        public boolean attemptWrite(int fileId) {
            // Check that this DomainThread can WRITE to prescribed file
            if (arbitrator(this, fileId, Perm.W)) {
                // Step into hashmap of <file id, <file semaphore, file string-data>>, and replace with new random string data
                files.get(fileId).replace(files.get(fileId).entrySet().iterator().next().getKey(), StringGenerator.generateString());
                out.println("DomainThread["+this.id + "] > '"+files.get(fileId).values()+"' written to File"+fileId);
                idle();
                return true;
            } else {
                out.println("DomainThread["+this.id + "] > Denied write-access to File"+fileId);
                idle();
                return false;
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




    public static void main(String[] args) {
        // Generate random values for N Domains and M Objects respectively.
        Random rand = new Random(System.currentTimeMillis());
        int nDomains = (abs(rand.nextInt()) % 5) + 3;
        int mObjects = (abs(rand.nextInt()) % 5) + 3;
        int totalEntities = nDomains + mObjects;
        out.println("numDomains: " + nDomains + "\nnumObjects: " + mObjects);

        // Declare and initialize Matrix entries with RANDOM permissions
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

        // Pack up the matrix data for printing TODO: // put into method called sendMatrixToPrinter()
        // Build title row
        ArrayList<String> titleRow = new ArrayList<>();
        titleRow.add("Object/Domain");
        for (int i=0; i < totalEntities; i++) {
            if (i < mObjects) {
                titleRow.add("FILE"+i);
            } else {
                titleRow.add("DOMAIN"+(i-mObjects));
            }
        }
        // Build row entries of matrix table data
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

        // Call Pretty Printer TODO: // put into method called printMatrix()
        final PrettyPrinter printer = new PrettyPrinter(out);
        printer.print(arrTableData);

        try {
            AccessMatrix am = new AccessMatrix(nDomains, mObjects, matrix);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}