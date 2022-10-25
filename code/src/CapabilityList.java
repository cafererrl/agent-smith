import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class CapabilityList {



    static class CList extends Thread{
        private int domain;
        private int files;

        private HashMap<Integer, String> permName;

        // Constructor
        public CList(int domain, int files, HashMap<Integer, String> permName){
            this.domain = domain;
            this.files = files;
            this.permName = permName;

        }

        // Create capability List
        void createCList(HashMap<Integer, HashMap> list){
            Random rand = new Random();

            for(int i = 0; i < domain ; i++){
                list.put(i, new HashMap<Integer, Integer>());
            }

            for(int i = 0; i < domain ; i++){
                for (int j = 0; j < files ; j++){
                    int perm = rand.nextInt(3,7);
                    if(perm != 6){
                        list.get(i).put(j, perm);
                    }
                    else{
                        list.get(i).put(j, null);
                    }
                }
            }

            for(int i = 0; i < domain ; i++){
                for(int j = files ; j < domain+files ; j++){
                    int perm = rand.nextInt(3);
                    if(perm == 1 & j != (files+i)){
                        list.get(i).put(j, perm);

                    }
                }
            }
        }

        // Print capability list
        void printCList(HashMap<Integer, HashMap> list){
            for (int i = 0; i < domain; i++) { // Domain has the outer indexes
                if(i < domain){
                    System.out.print("D"+i+"-> ");
                }
                for (int j = 0; j < files+domain; j++) { // Inner indexes are filled with files then domains for switches
                    Object perm = list.get(i).get(j);
                    if(perm != null) {
                        if(j >= 0 & j < files){
                            System.out.printf("F%d:%-15s",j,permName.get((Integer)perm));
                        }
                        else{
                            System.out.printf("D%d:%-15s",(j-files),permName.get((Integer)perm));
                        }

                    }
                }
                System.out.println();
            }
            System.out.println();
        }

    }

    static class CLThreads extends Thread{
        int domain;

        int files;
        int threadId;

        HashMap<Integer, HashMap> list;
        Semaphore mutex = new Semaphore(1);
        Random rand = new Random();
        String[] fileStuff;

        public static void yieldTime(){
            Random rand = new Random();
            int low = 3;
            int high = 6;
            int result = rand.nextInt(high-low) + low;
            for (int j = 0; j < result; j++){
                Thread.yield();
            }
        }

        public CLThreads(int threadId, int files, HashMap<Integer, HashMap> list){
            this.threadId = threadId;
            this.domain = threadId;
            this.files = files;
            this.list = list;
            fileStuff = new String[files];
        }

        public int[] getOperations(){
            int thread = rand.nextInt(files);
            int operation = rand.nextInt(5-3) + 3;
            int[] array = {thread , operation};
            return array;
        } // Get R/W, R, W operations

        public Boolean checkCList(int thread, int op, HashMap<Integer, HashMap> list){
            if(list.get(domain).get(thread) != null && list.get(domain).get(thread).equals(1)){
                return true;
            }if(list.get(domain).get(thread) != null && (list.get(domain).get(thread).equals(op) || list.get(domain).get(thread).equals(5))){
                return true;
            }
            return false;
        } // Check if file/domain has permissions

        public String generateString() {
            byte[] array = new byte[3]; // length is bounded by 7
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));
            return generatedString;
        } // Generate random string to write



        @Override
        public void run(){
            System.out.println("Domain " + domain + " has arrived");

            for(int i = 0; i < domain ; i++){
                int operation = rand.nextInt(10);
                if(operation == 5){
                    //  domain switch
                    int dSwitch = rand.nextInt((domain + files) - files) + files;
                    if(checkCList(dSwitch, operation, list)){
                        try {
                            mutex.acquire();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("D" + domain + " is trying to switch to D" + (dSwitch-files));
                        yieldTime();
                        mutex.release();
                        yieldTime();
                    }
                    else{
                        try {
                            mutex.acquire();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("D" + domain + " is trying to switch to D" + (dSwitch-files) + "- Permission denied");
                        yieldTime();
                        mutex.release();
                        yieldTime();
                    }

                }
                else{
                    // File operations
                    int[] array = getOperations();
                    int file = array[0];
                    int fileOp = array[1];

                    if(checkCList(file, fileOp, list)){
                        if(fileOp == 3){
                            int wordType = rand.nextInt(files);
                            try {
                                mutex.acquire();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println("D" + domain + " is trying to read from F" + wordType +":(" + fileStuff[wordType] + ")");
                            yieldTime();
                            mutex.release();
                            yieldTime();
                        }

                        else if(fileOp == 4){
                            try {
                                mutex.acquire();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            fileStuff[file] = generateString();
                            System.out.println("D" + domain + " is trying to write to F" + file +":(" + fileStuff[file] + ")");
                            yieldTime();
                            mutex.release();
                            yieldTime();
                        }
                    }
                    else{
                        if(fileOp == 3){
                            try {
                                mutex.acquire();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println("D" + domain + " is trying to read from F" + file +" - Permission denied");
                            mutex.release();
                        }
                        else if(fileOp == 4){
                            try {
                                mutex.acquire();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println("D" + domain + " is trying to write to F" + file +" - Permission denied");
                            mutex.release();
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        HashMap<Integer, HashMap> capabilityList = new HashMap<>();
        HashMap<Integer, String> permName = new HashMap<>(); // Permissions Map
        permName.put(3,"Read");
        permName.put(4,"Write");
        permName.put(5,"Read/Write");
        permName.put(1,"True");


        Random rand = new Random();
        int domain = rand.nextInt(7-3) + 3; // generate random domain
        int files = rand.nextInt(7-3) + 3; // generate random files

        System.out.println(domain + " domains");
        System.out.println(files + " files");

        CList list = new CList(domain, files, permName); // Create capability list

        list.createCList(capabilityList);

        list.printCList(capabilityList); // Print capability list

        CLThreads[] threads = new CLThreads[domain]; // Send threads

        for(int i = 0; i < domain ; i++){
            threads[i] = new CLThreads(i, files, capabilityList);
            threads[i].start();
        }

    }

}
