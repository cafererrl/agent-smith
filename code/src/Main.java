import Tasks.*;

/*
Command line:

    Compile

            javac Main.java

    Execute

            java Main <arg1> <arg2>

            // where arg1 is "-S" and arg2 is 1, 2, or 3 for the
            // desired task to run
 */

public class Main {

    public static void main(String[] args) {

        // check if there's too little or too many arguments
        if (args.length <=0){
            System.out.println("You cannot have 0 args");
            System.exit(0);
        }
        else if (args.length <= 1){
            System.out.println("You must pass another argument.");
            System.exit(0);
        }
        else if(args.length > 2){
            System.out.println("Error: too many arguments. Expected 2");
            System.exit(0);
        }
        else { // they have the correct number of arguments
            if(args[0].equals("-S")){ // they have entered the correct arg1
                if(args[1].equals("1")){ // run task 1
                    Task1.main();
                }
                else if(args[1].equals("2")){
                    Task2.main();
                }
                else if(args[1].equals("3")){
                    Task3.main();
                }else{
                    // let them know that they enetered an invalid number
                    System.out.format("\"%s\" is not a valid option. \n" +
                            "--Options are 1, 2, or 3.\n", args[1]);
                    System.exit(0);
                }

            }else{ // incorect arg1
                System.out.format("\"%s\" is not a recognized argument, please try again.\n" +
                        "--Recognized args: \"-S\"\n", args[1]);
                System.exit(0);
            }
        }

    }

}