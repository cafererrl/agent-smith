import java.util.*;
import static java.lang.Math.abs;
import static java.lang.System.out;

public class AccessMatrix {
    // Declare Permissions. Lookup for Domains and Objects.
    enum Perm {
        NULL,           // 0 NULL
        R,              // 1 READ
        W,              // 2 WRITE
        RW,             // 3 READ-WRITE
        SwitchAllow,   // 4 TRUE
        SwitchDeny     // 5 FALSE
    }


    // Constructor
    private AccessMatrix(int numDomains, int numObjects, Map<Integer, Map<Integer, Perm>> matrix) throws InterruptedException {
        // instantiate Threads who will try to access the matrix data
    }




    public static void main(String[] args) {
        // Generate random values for N Domains and M Objects respectively.
        Random rand = new Random(System.currentTimeMillis());
        int nDomains = (abs(rand.nextInt()) % 5) + 3;
        int mObjects = (abs(rand.nextInt()) % 5) + 3;
        int totalEntities = nDomains + mObjects;
        out.println("numDomains: " + nDomains + "\nnumObjects: " + mObjects);

        // Populate random strings as "File" Objects
        HashMap<Integer, String> myFiles = new HashMap<Integer, String>();
        for (int i=0; i < mObjects; i++) {
            myFiles.put(i, StringGenerator.generateString());
        }
        // Print object-file contents TODO DO NOT DELETE
        /*out.println("\nMy Files:");
        for (int i=0; i < myFiles.size(); i++) {
            out.println("File "+i+": "+myFiles.get(i));
        }*/

        // Declare and initialize Matrix entries with RANDOM permissions
        Map<Integer, Map<Integer, Perm>> matrix = new HashMap<Integer, Map<Integer, Perm>>();
        for (int domainKey=0; domainKey < nDomains; domainKey++) {
            matrix.put(domainKey, new HashMap<Integer, Perm>());
            for (int entityKey=0; entityKey < totalEntities; entityKey++) {
                if (entityKey < mObjects) {
                    // Generate only NULL/READ/WRITE/READWRITE perm on the file columns of the matrix (0-3)
                    int randZeroThruThree = (abs(new Random().nextInt()) % 4);
                    matrix.get(domainKey).put(entityKey, Perm.values()[randZeroThruThree]);
                } else {
                    if (domainKey == (entityKey-mObjects)) {
                        // Generate Domain Switch DENY Perm across Domain diagonal. Domains can't switch to the same Domain (5)
                        matrix.get(domainKey).put(entityKey, Perm.SwitchDeny);
                    } else {
                        // Generate only either DSwitchAllow or DSwitchDeny Perm (4,5)
                        int randFourFive = (abs(new Random().nextInt()) % 2) + 4;
                        matrix.get(domainKey).put(entityKey, Perm.values()[randFourFive]);
                    }
                }
            }
        }

        // Get matrix, for debugging
//        for (Integer domainKey: matrix.keySet()) {
//            Map<Integer, Perm> innerSet = matrix.get(domainKey);
//            for (Integer entityKey: innerSet.keySet()) {
//                Perm value = innerSet.get(entityKey);
//                out.println(">> Domain"+ domainKey+"| File/Domain Entity"+entityKey+": "+value);
//            }
//        }

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