import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws Exception {
    try (var scanner = new Scanner(System.in)) {
      while (true) {
        System.out.print("$ ");
        var command = scanner.nextLine();
        var error = String.format("%s: command not found", command);
        System.out.println(error);
      }
    }
  }
}
