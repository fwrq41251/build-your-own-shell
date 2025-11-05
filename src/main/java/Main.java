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

  record Command(String command, int arg) {
  }

  private static Command parse(String command) {
    if (command == null || command.isEmpty()) {
      throw new IllegalArgumentException("command cannot be null or empty");
    }
    var args = command.split(" ");
    var arg = args.length > 1 ? Integer.parseInt(args[1]) : -1;
    return new Command(args[0], arg);
  }

  private static void run(Command command) {
    switch (command.command) {
      case "exit" -> {
        System.exit(command.arg);
      }
      default -> {
        var error = String.format("%s: command not found", command.command);
        System.out.println(error);
      }
    }
  }
}
