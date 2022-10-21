

import java.io.File;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Task2 {
    protected ArrayList<LinkedList> acl;
    private HashMap<String, Integer> permissions = new HashMap<String, Integer>();
    private int domainNum;
    private int fileNum;
    private static Semaphore[] fileProtection;

    private Task2(){
        int totalNum = fileNum + domainNum;
        fileProtection = new Semaphore[totalNum];
        aclThread[] aclThreads = new aclThread[domainNum];

        generateList();
        for (int i=fileNum; i<totalNum; i++){
            fileProtection[i] = new Semaphore(1);
            aclThreads[i] = new aclThread(i, fileNum, domainNum, acl);
            aclThreads[i].start();
        }
        System.out.println(acl);
    }

    private void generateList(){
        this.domainNum = ThreadLocalRandom.current().nextInt(3, 8);
        this.fileNum = ThreadLocalRandom.current().nextInt(3, 8);
        fillPermissions();
        fillList();
    }

    private void fillPermissions(){
        this.permissions.put(null, 0);
        this.permissions.put("True", 1);
        this.permissions.put("False", 2);
        this.permissions.put("Read", 3);
        this.permissions.put("Write", 4);
        this.permissions.put("Read and Write", 5);
    }

    private void fillList(){
        this.acl = new ArrayList<>();
        // populate the headers of the linked list
        for (int i=0; i<(fileNum + domainNum); i++){
            LinkedList ll = new LinkedList<>();
            this.acl.add(ll);
            // for each domain, pick a random number to rep whether domain can R/W to file
            for (int j=0; j<this.domainNum; j++){
                if (i < fileNum){ // if we're talking about permissions to a file
                    int domainPermission = ThreadLocalRandom.current().nextInt(3, 6);
                    this.acl.get(i).add(domainPermission);
                }
                else { // we're talking about permissions to domain switch
                    if (i == (j-fileNum)){ // domain cannot switch to itself
                        this.acl.get(i).add(0);
                    }
                    else{
                        int domainPermission = ThreadLocalRandom.current().nextInt(1,3);
                        this.acl.get(i).add(domainPermission);
                    }

                }

            }
        }

    }

    static class aclThread extends Thread{
        int domainId; //corresponds to the fileNum
        int fileNum;
        int domainNum;
        ArrayList<LinkedList> acl;

        // constructor
        public aclThread(int domain, int fileNum, int domainNum, ArrayList<LinkedList> acl){
            domainId = domain;
            this.fileNum = fileNum;
            this.domainNum = domainNum;
            this.acl = acl;

        }
        @Override
        public void run(){
            for (int i=0; i<5; i++){
                int[] array = getOperation();// get operation on file/obj
                int idx = array[0]; // where we're going
                int operation = array[1]; // what operation is being performed

                if (idx < fileNum){
                    if(((int)acl.get(idx).get(domainId) == operation) || ((int)acl.get(idx).get(domainId)==5)){
                        // DO
                    }

                }
                else{ // looking to switch to another domain
                    if((int)acl.get(idx).get(domainId) == operation){
                        // DO 
                    }

                }

                // after performing operation, yield
                int yieldNums = ThreadLocalRandom.current().nextInt(3, 8);
                for (int j=0; j<yieldNums; j++){
                    Thread.yield();
                }
            }
        }

        private int[] getOperation(){
            int totalNum = fileNum + domainNum;
            int idx = ThreadLocalRandom.current().nextInt(0, totalNum);
            int operation;
            if (idx < fileNum){ // performing a R/W
                operation = ThreadLocalRandom.current().nextInt(3, 5);
            }else{ // performing a switch
                operation = 2;
            }
            return new int[]{idx, operation};
        }
    }

    public static void main(String[] args){

        try{
            Task2 t2 = new Task2();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
