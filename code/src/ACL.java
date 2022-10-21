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
    static Semaphore mutex;

    public ACL(int randDomain, int randFile){
        //random number for access control
        rand = new Random();
        domains = randDomain;
        files = randFile;
        accessList = new HashMap<>();
        fileContent = new String[randFile];

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
                if(perm == 1){
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
                    System.out.print("D" + j + ":" + perm + "   ");
                }
            }

            System.out.println();
        }
    }


    class ACLThreads extends Thread{
        int id;


        public ACLThreads(int id){
            this.id = id;


        }

        @Override
        public void run(){
            int operation;
            int fileOp;
            String newContent;
            for(int i = 0; i< 5; i++){
                operation = rand.nextInt(3,5);
                fileOp = rand.nextInt(files);
                if(operation != 0 && checkACL(fileOp, operation)){
                    if(operation == 3){
                        mutex.acquireUninterruptibly();
                        System.out.println("D"+id+": read file"+fileOp+" '"+fileContent[fileOp]+"'");
                        mutex.release();
                    }
                    else if(operation == 4){
                        newContent = sentences[rand.nextInt(3)];
                        mutex.acquireUninterruptibly();
                        fileContent[fileOp] = newContent;
                        System.out.println("D"+id+": wrote to file"+fileOp+" '"+newContent+"'");
                        mutex.release();
                    }

                }
                else{
                    if(operation == 3){
                        mutex.acquireUninterruptibly();
                        System.out.println("D"+id+": tried to read file"+fileOp+". Permission Denied!");
                        mutex.release();
                    }
                    else if(operation == 4){
                        mutex.acquireUninterruptibly();
                        System.out.println("D"+id+": tried to write to file"+fileOp+". Permission Denied!");
                        mutex.release();
                    }


                }

            }

        }

        public Boolean checkACL(int objNum, Integer operation){
            //checking if they have permission
            Object perm = accessList.get(objNum).get(this.id);
            if(perm != null && (perm == operation || perm == (Integer)5)) {
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

