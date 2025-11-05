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

  enum CommandType {
    exit, unknown
  }

  record Command(CommandType command, int arg) {
  }

  private static Command parse(String command) {
    var args = command.split(" ");
    var arg = Integer.parseInt(args[1]);
    var commandType = CommandType.valueOf(args[0]);
    if (commandType == null) {
      commandType = CommandType.unknown;
    }
    return new Command(commandType, arg);
  }

  private static void run(Command command) {
    switch (command.command) {
      case unknown -> {
        var error = String.format("%s: command not found", command);
        System.out.println(error);
      }
      case exit -> {
        System.exit(command.arg);
      }
    }
  }
}
