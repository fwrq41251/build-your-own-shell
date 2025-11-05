import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
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

  enum CommandName {
    exit, echo, type;

    static CommandName of(String name) {
      try {
        return valueOf(name);
      } catch (IllegalArgumentException e) {
        return null;
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
    var commandName = CommandName.of(command.command);

    if (Objects.isNull(commandName)) {
      var error = String.format("%s: command not found", command.command);
      System.out.println(error);
      return;
    }

    switch (commandName) {
      case exit -> {
        int status = 0;
        if (command.args.length != 0) {
          status = Integer.parseInt(command.args[0]);
        }
        System.exit(status);
      }
      case echo -> {
        var message = String.join(" ", command.args);
        System.out.println(message);
      }
      case type -> {
        runType(command);
      }
    }
  }

  private static void runType(Command command) {
    if (command.args.length == 0) {
      System.out.println("type command requires an argument");
      return;
    }
    var arg0 = command.args[0];
    var toType = CommandName.of(arg0);
    if (toType == null) {
      var executable = findExecutable(arg0);
      if (executable != null) {
        var message = String.format("%s is %s", arg0, executable);
        System.out.println(message);
      } else {
        var error = String.format("%s: not found", arg0);
        System.out.println(error);
      }
    } else {
      var message = String.format("%s is a shell builtin", toType);
      System.out.println(message);
    }
  }

  private static String findExecutable(String commandName) {
    String pathEnv = System.getenv("PATH");
    String[] directories = pathEnv.split(System.getProperty("path.separator"));

    for (String dir : directories) {
      var filePath = Paths.get(dir, commandName);
      if (Files.isExecutable(filePath)) {
        return filePath.toAbsolutePath().toString();
      }
    }

    return null;
  }

}
