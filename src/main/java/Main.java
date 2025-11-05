import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws Exception {
    System.out.print("$ ");
    try (var scanner = new Scanner(System.in)) {
      var command = scanner.nextLine();
      var error = String.format("%s: command not found", command);
      System.out.println(error);
    }
  }
}
