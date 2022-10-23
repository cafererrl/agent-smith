import Tasks.*;


public class Main {

    public static void main(String[] args) {
        for(int i=0; i< args.length; i++){
            System.out.format("\nCommand Line Argument %d is %s", i, args[i]);
        }
        Task1.main();
        Task2.main();
        Task3.main();

    }

}