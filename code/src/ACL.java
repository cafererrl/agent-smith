import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class ACL {
    private HashMap<Integer,HashMap> accessList;
    private int[] perms = {0,1,2,3,4,5};
    private int domains;
    private int files;
    private Random rand;
    private String [] fileContent;
    private String [] sentences;
    private HashMap<Integer, String> permName;
    static Semaphore mutex;

    public ACL(int randDomain, int randFile){
        //random number for access control
        rand = new Random();
        domains = randDomain;
        files = randFile;
        accessList = new HashMap<>();
        fileContent = new String[randFile];
        permName = new HashMap<>();
        permName.put(3,"Read");
        permName.put(4,"Write");
        permName.put(5,"Read/Write");
        permName.put(1,"True");

        sentences = new String[]{"Hello!","That's really nice village","Its cloudy today","Star Wars is a good movie"};

        for(int i = 0; i < (files + domains); i++){
            // making empty lists
            //objects accessed 0-n where n corresponds to the object number
            //files will be first followed by domains
            accessList.put(i,new HashMap<Integer, Integer>());
        }
        //setting file values
        for(int i = 0; i < files; i++){
            HashMap file = accessList.get(i);
            for(int j = 0; j < domains; j++){
                // if perm = 6 then the domain will not be put in list
                //AKA 6 means it has no permissions
                int perm = rand.nextInt(3,7);
                if(perm != 6){
                    file.put(j,perm);
                }
            }
        }
        //setting domain switch values
        for(int i = files; i < (files + domains); i++){
            HashMap domain = accessList.get(i);
            for(int j = 0; j < domains; j++){
                int perm = rand.nextInt(3);
                // if it gets the permission and the domain isn't itself
                if(perm == 1 && j != (i - files)){
                    domain.put(j,perm);
                }
            }
        }
        //printing out the ACL
        this.printACL();

        //intializing sem
        ACL.mutex = new Semaphore(1);

        //Starting the Domain threads
        ACLThreads[] aclThreads = new ACLThreads[domains];

        for(int i =0; i < domains; i++){

            //starting Threads
            aclThreads[i] = new ACLThreads(i);
            aclThreads[i].start();
        }


    }

    public void printACL(){
        int total = this.domains + this.files;

        for (int i = 0; i < total; i++) {
            if(i < this.files){
                System.out.print("F"+i+"-> ");
            }
            else{
                System.out.print("D"+(i-this.files)+"-> ");
            }
            for (int j = 0; j < this.domains; j++) {
                Object perm = this.accessList.get(i).get(j);
                if(perm != null) {
                    System.out.printf("D%d:%-15s",j,permName.get((Integer)perm));
                }
            }

            System.out.println();
        }
        System.out.println();
    }


    class ACLThreads extends Thread{
        int id;
        int domain;


        public ACLThreads(int id){
            this.id = id;
            domain = id;


        }

        @Override
        public void run(){
            int operation;
            int fileOp;
            String newContent;
            int domainSW;
            for(int i = 0; i< 5; i++){
                //deciding if it will attempt a domain switch
                if(rand.nextInt(10) == 5){
                    domainSW = rand.nextInt(files, (files+domains));
                    //1 means its trying to domain switch
                    if(checkACL(domainSW,1) && (domainSW-files) != domain){
                        mutex.acquireUninterruptibly();
                        System.out.println("U"+id+"(D"+domain+"): switched to domain D" +(domainSW -files));
                        domain = (domainSW - files);
                        mutex.release();
                    }
                    //ensure a domain doesn't try to switch to its self
                    else if((domainSW - files) == domain){
                        continue;
                    }
                    else{
                        mutex.acquireUninterruptibly();
                        System.out.println("U"+id+"(D"+domain+"): attempted to switch to domain D" +(domainSW - files)+ ". Permission Denied!");
                        mutex.release();
                    }
                }

                //file operations
                operation = rand.nextInt(3,5);
                fileOp = rand.nextInt(files);
                //using arbitrator to check permissions of file
                if(operation != 0 && checkACL(fileOp, operation)){
                    if(operation == 3){
                        mutex.acquireUninterruptibly();
                        System.out.println("U"+id+"(D"+domain+"): read file"+fileOp+" '"+fileContent[fileOp]+"'");
                        mutex.release();
                    }
                    else if(operation == 4){
                        newContent = sentences[rand.nextInt(3)];
                        mutex.acquireUninterruptibly();
                        fileContent[fileOp] = newContent;
                        System.out.println("U"+id+"(D"+domain+"): wrote to file"+fileOp+" '"+newContent+"'");
                        mutex.release();
                    }

                }
                else{
                    if(operation == 3){
                        mutex.acquireUninterruptibly();
                        System.out.println("U"+id+"(D"+domain+"): tried to read file"+fileOp+". Permission Denied!");
                        mutex.release();
                    }
                    else if(operation == 4){
                        mutex.acquireUninterruptibly();
                        System.out.println("U"+id+"(D"+domain+"): tried to write to file"+fileOp+". Permission Denied!");
                        mutex.release();
                    }


                }

            }

        }

        public Boolean checkACL(int objNum, Integer operation){
            //checking if they have permission
            Object perm = accessList.get(objNum).get(this.domain);
            //checks read write permissions
            if(perm != null && (perm == operation || perm == (Integer)5)) {
                return true;
            }
            //checks domain switch permissions
            else if(perm != null && perm == (Integer)1){
                return true;
            }
            return false;
        }
    }


    public static void main(String[] args) {
        Random rand = new Random();
        ACL test = new ACL(rand.nextInt(3,7), rand.nextInt(3,7));



    }
}

