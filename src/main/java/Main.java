import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws Exception {
    try (var scanner = new Scanner(System.in)) {
      while (true) {
        System.out.print("$ ");
        var command = parse(scanner.nextLine());
        run(command);
      }
    }
  }

  record Command(String command, String[] args) {
  }

  private static Command parse(String command) {
    if (command == null || command.isEmpty()) {
      throw new IllegalArgumentException("command cannot be null or empty");
    }
    var split = command.split(" ");
    var args = split.length > 1 ? Arrays.copyOfRange(split, 1, split.length) : new String[0];
    return new Command(split[0], args);
  }

  private static void run(Command command) {
    switch (command.command) {
      case "exit" -> {
        int status = 0;
        if (command.args.length != 0) {
          status = Integer.parseInt(command.args[0]);
        }
        System.exit(status);
      }
      case "echo" -> {
        var message = String.join(" ", command.args);
        System.out.println(message);
      }
      default -> {
        var error = String.format("%s: command not found", command.command);
        System.out.println(error);
      }
    }
  }
}
