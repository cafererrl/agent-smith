import java.util.Random;
// The following contribution was used in this project:
// Random String Generation (Baeldung) https://www.baeldung.com/java-random-string

public final class StringGenerator {
    static int leftLimit = 97; // letter 'a'
    static int rightLimit = 122; // letter 'z'
    static int targetStringLength = 8; // string length
    static Random random = new Random();

    public static String generateString() {
        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
