package org.winry;

import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class Main {
    private static final String HOME = "~";
    private static final String PATH = "PATH";
    private static Path PWD = Paths.get(System.getProperty("user.dir"));
    private static List<String> historyList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        var terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        var parser = new DefaultParser();
        parser.setEscapeChars(new char[0]);

        var executables = findExecutables();
        var completer = new MyCompleter(executables);
        var lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .parser(parser)
                .build();

        String prompt = "$ ";

        while (true) {
            String line = lineReader.readLine(prompt);

            if (line != null && !line.isEmpty()) {
                historyList.add(line);
                var commandLine = parse(line);
                run(commandLine);
            }
        }
    }

    enum BuiltInCommand implements RunBuiltin {
        exit {
            @Override
            public void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception {
                int status = 0;
                if (args.length != 0) {
                    status = Integer.parseInt(args[0]);
                }
                System.exit(status);
            }
        }, echo {
            @Override
            public void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception {
                var message = String.join(" ", args);
                write(out, message);
            }
        }, type {
            @Override
            public void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception {
                if (args.length == 0) {
                    write(out, "type command requires an argument");
                    return;
                }
                var arg0 = args[0];
                var toType = BuiltInCommand.of(arg0);
                if (toType == null) {
                    var executable = findExecutable(arg0);
                    if (executable != null) {
                        var message = String.format("%s is %s", arg0, executable);
                        write(out, message);
                    } else {
                        var error = String.format("%s: not found", arg0);
                        write(out, error);
                    }
                } else {
                    var message = String.format("%s is a shell builtin", toType);
                    write(out, message);
                }
            }
        }, pwd {
            @Override
            public void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception {
                var message = PWD.toAbsolutePath().toString();
                write(out, message);
            }
        }, cd {
            @Override
            public void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception {
                if (args.length == 0) {
                    return;
                }
                var targetPath = args[0];
                var separator = FileSystems.getDefault().getSeparator();
                if (targetPath.equals(HOME) || targetPath.startsWith(HOME + separator)) {
                    var homeDir = System.getenv("HOME");
                    targetPath = targetPath.replaceFirst(HOME, homeDir);
                }

                var newPath = PWD.resolve(targetPath).normalize();
                if (!Files.isDirectory(newPath)) {
                    var error = String.format("cd: %s: No such file or directory", newPath);
                    write(out, error);
                } else {
                    PWD = newPath;
                }
            }
        }, history {;

            @Override
            public void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception {
                var limit = historyList.size();
                if (args.length > 0) {
                    var arg0 = args[0];
                    if (arg0.equals("-r")) {
                        var arg1 = args[1];
                        var historyFile = Path.of(arg1);
                        historyList.addAll(Files.readAllLines(historyFile));
                        return;
                    }
                    if (isInteger(arg0)) {
                        limit = Integer.parseInt(args[0]);
                    }
                }
                limit = Math.min(limit, historyList.size());
                var start = Math.max(0, historyList.size() - limit);
                for (int i = start; i < historyList.size(); i++) {
                    var entry = String.format("%d  %s", i + 1, historyList.get(i));
                    write(out, entry);
                }
            }
        };

        static BuiltInCommand of(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }

    private static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void write(OutputStream out, String message) throws IOException {
        out.write((message + "\n").getBytes());
        out.flush();
    }

    record Command(String command, String[] args) {

        public String[] getCommandWithArgs() {
            String[] commandWithArgs = new String[args.length + 1];
            commandWithArgs[0] = command;
            System.arraycopy(args, 0, commandWithArgs, 1, args.length);
            return commandWithArgs;
        }

    }

    private static CommandLine parse(String command) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("command cannot be null or empty");
        }

        var commandLine = new CommandLine();

        var tokens = splitCommand(command);
        var split = new ArrayList<List<String>>();
        var toSplit = List.of("|", ">", ">>", "1>", "1>>", "2>", "2>>");
        var temp = new ArrayList<String>();
        var splitToken = "";
        for (var token : tokens) {
            if (toSplit.contains(token)) {
                if (!temp.isEmpty()) {
                    split.add(new ArrayList<>(temp));
                    temp.clear();
                    if (!token.equals("|")) {
                        splitToken = token;
                    } else {
                        splitToken = "";
                    }
                }
            } else {
                if (!splitToken.isEmpty()) {
                    switch (splitToken) {
                        case ">", "1>" -> {
                            commandLine.outRedirect = token;
                            commandLine.outAppend = false;
                        }
                        case ">>", "1>>" -> {
                            commandLine.outRedirect = token;
                            commandLine.outAppend = true;
                        }
                        case "2>" -> {
                            commandLine.errRedirect = token;
                            commandLine.errAppend = false;
                        }
                        case "2>>" -> {
                            commandLine.errRedirect = token;
                            commandLine.errAppend = true;
                        }
                        default ->
                                throw new IllegalArgumentException("Unknown redirection operator: " + splitToken);
                    }
                } else {
                    temp.add(token);
                }
            }
        }
        split.add(temp);
        commandLine.commands = split.stream()
                .filter(s -> !s.isEmpty())
                .map(Main::getCommand)
                .toList();
        return commandLine;
    }

    private static Command getCommand(List<String> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("command cannot be empty");
        }

        // no args
        if (tokens.size() == 1) {
            return new Command(tokens.getFirst(), new String[0]);
        }

        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);
        return new Command(tokens.getFirst(), args);
    }

    private enum QuteMode {
        singleQuote, doubleQuote
    }

    private static List<String> splitCommand(String command) {
        var result = new ArrayList<String>();
        var temp = new StringBuilder();
        QuteMode quteMode = null;
        var escape = false;
        var toEscape = Set.of('\"', '\\', '$', '`');

        for (char ch : command.toCharArray()) {
            if (quteMode == QuteMode.singleQuote) {
                if (ch == '\'') {
                    quteMode = null;
                } else {
                    temp.append(ch);
                }
            } else if (quteMode == QuteMode.doubleQuote) {
                if (escape) {
                    if (!toEscape.contains(ch)) {
                        temp.append('\\');
                    }
                    temp.append(ch);
                    escape = false;
                } else {
                    if (ch == '\"') {
                        quteMode = null;
                    } else if (ch == '\\') {
                        escape = true;
                    } else {
                        temp.append(ch);
                    }
                }
            } else {
                if (escape) {
                    temp.append(ch);
                    escape = false;
                } else {
                    if (ch == '\'') {
                        quteMode = QuteMode.singleQuote;
                    } else if (ch == '\"') {
                        quteMode = QuteMode.doubleQuote;
                    } else if (ch == ' ') {
                        addTemp(result, temp);
                    } else if (ch == '\\') {
                        escape = true;
                    } else {
                        temp.append(ch);
                    }
                }
            }
        }

        if (quteMode != null) {
            throw new IllegalArgumentException("Unclosed quote.");
        }

        addTemp(result, temp);

        return result;
    }

    private static void addTemp(List<String> result, StringBuilder temp) {
        if (!temp.isEmpty()) {
            result.add(temp.toString());
            temp.setLength(0);
        }
    }

    private static OutputStream getFinalOutputStream(CommandLine commandLine) throws FileNotFoundException {
        if (commandLine.outRedirect != null) {
            var file = Path.of(commandLine.outRedirect).toFile();
            return new FileOutputStream(file, commandLine.outAppend);
        } else {
            return System.out;
        }
    }

    private static OutputStream getFinalErrorStream(CommandLine commandLine) throws FileNotFoundException {
        if (commandLine.errRedirect != null) {
            var file = Path.of(commandLine.errRedirect).toFile();
            return new FileOutputStream(file, commandLine.errAppend);
        } else {
            return System.err;
        }
    }

    private static void run(CommandLine commandLine) throws Exception {
        List<Command> commands = commandLine.commands;
        boolean hasBuiltin = false;
        List<ProcessBuilder> processBuilders = new ArrayList<>();

        for (var command : commands) {
            if (BuiltInCommand.of(command.command) != null) {
                hasBuiltin = true;
                processBuilders.add(null); // 用 null 作为内置命令的占位符
            } else {
                var executable = findExecutable(command.command);
                if (executable != null) {
                    processBuilders.add(new ProcessBuilder(command.getCommandWithArgs()));
                } else {
                    System.out.println(command.command + ": command not found");
                    return; // 任何一个命令找不到，整个管道就失败
                }
            }
        }


        if (!hasBuiltin) {
            // ---- 策略 A: 纯外部命令管道 (最简单的情况) ----
            executeExternalPipeline(processBuilders, commandLine);
        } else {
            // ---- 策略 B: 包含内置命令的混合管道 (复杂的情况) ----
            executeMixedPipeline(commands, commandLine);
        }
    }

    private static void executeExternalPipeline(List<ProcessBuilder> processBuilders, CommandLine commandLine)
            throws IOException, InterruptedException {
        var lastPb = processBuilders.getLast();
        if (commandLine.outRedirect != null) {
            var file = new File(commandLine.outRedirect);
            if (commandLine.outAppend) {
                lastPb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
            } else {
                lastPb.redirectOutput(file);
            }
        } else {
            lastPb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        if (commandLine.errRedirect != null) {
            var file = new File(commandLine.errRedirect);
            if (commandLine.errAppend) {
                lastPb.redirectError(ProcessBuilder.Redirect.appendTo(file));
            } else {
                lastPb.redirectError(file);
            }
        } else {
            lastPb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }

        List<Process> processes = ProcessBuilder.startPipeline(processBuilders);

        var lastProcess = processes.getLast();
        lastProcess.waitFor();
    }


    private static void executeMixedPipeline(List<Command> commands, CommandLine commandLine) throws Exception {
        InputStream nextInputStream = System.in;

        for (int i = 0; i < commands.size(); i++) {
            var command = commands.get(i);
            boolean isLastCommand = (i == commands.size() - 1);

            // 用来连接到下一个命令的管道
            PipedOutputStream pipeOut = new PipedOutputStream();
            // 如果不是最后一个命令，下一个命令的输入就是这个管道的输入端
            InputStream downstreamInput = isLastCommand ? null : new PipedInputStream(pipeOut);

            // 决定当前命令的输出目标
            OutputStream currentOutputStream;
            if (isLastCommand) {
                currentOutputStream = getFinalOutputStream(commandLine); // 可能是 System.out 或文件
            } else {
                currentOutputStream = pipeOut; // 输出到管道
            }

            OutputStream currentErrorStream = getFinalErrorStream(commandLine); // 简化：所有错误都去最终目的地

            var buildIn = BuiltInCommand.of(command.command);
            if (buildIn != null) {
                // -- 执行内置命令 --
                try {
                    // 内置命令是同步的，直接在当前线程执行
                    buildIn.run(command.args, nextInputStream, currentOutputStream, currentErrorStream);
                } finally {
                    if (currentOutputStream != System.out) {
                        // 执行完后必须关闭输出流，以通知下游 EOF
                        currentOutputStream.close();
                    }
                }
            } else {
                // -- 执行外部命令 --
                var processBuilder = new ProcessBuilder(command.getCommandWithArgs());
                var process = processBuilder.start();

                // 使用虚拟线程并发处理 I/O
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    final InputStream finalNextInputStream = nextInputStream;
                    if (finalNextInputStream != System.in) {
                        // 如果输入来自上一个命令的管道，则正常泵送
                        executor.submit(() -> {
                            try (OutputStream os = process.getOutputStream()) {
                                finalNextInputStream.transferTo(os);
                            } catch (IOException e) { /* ... */ }
                        });
                    } else {
                        // 如果输入是 System.in，我们不启动泵送线程，
                        // 而是直接关闭子进程的输入流，因为它不需要
                        process.getOutputStream().close();
                    }

                    final OutputStream finalCurrentOutputStream = currentOutputStream;
                    executor.submit(() -> {
                        try (InputStream is = process.getInputStream()) {
                            is.transferTo(finalCurrentOutputStream);
                        } catch (IOException e) { /* ... */ }
                    });

                    final OutputStream finalCurrentErrorStream = currentErrorStream;
                    executor.submit(() -> {
                        try (InputStream is = process.getErrorStream()) {
                            is.transferTo(finalCurrentErrorStream);
                        } catch (IOException e) { /* ... */ }
                    });

                    process.waitFor(); // 等待当前进程结束
                } finally {
                    if (currentOutputStream != System.out) {
                        // 执行完后必须关闭输出流，以通知下游 EOF
                        currentOutputStream.close();
                    }
                }
            }

            // 为下一次循环准备 "接力棒"
            if (!isLastCommand) {
                nextInputStream = downstreamInput;
            }
        }
    }

    interface RunBuiltin {
        void run(String[] args, InputStream in, OutputStream out, OutputStream err) throws Exception;
    }

    private static class CommandLine {
        List<Command> commands;
        String outRedirect;
        String errRedirect;
        boolean outAppend;
        boolean errAppend;
    }

    private static String findExecutable(String commandName) {
        var pathEnv = System.getenv(PATH);
        var directories = pathEnv.split(System.getProperty("path.separator"));

        for (var dir : directories) {
            var filePath = Paths.get(dir, commandName);
            if (Files.isExecutable(filePath)) {
                return filePath.toAbsolutePath().toString();
            }
        }

        return null;
    }

    private static List<String> findExecutables() {
        var pathEnv = System.getenv(PATH);
        var directories = pathEnv.split(System.getProperty("path.separator"));
        var executables = new ArrayList<String>();

        for (var dir : directories) {
            var directoryPath = Paths.get(dir);
            if (Files.isDirectory(directoryPath)) {
                try (var stream = Files.list(directoryPath)) {
                    stream.filter(Files::isExecutable)
                            .forEach(path -> executables.add(path.getFileName().toString()));
                } catch (IOException e) {
                    // Ignore directories that cannot be read
                }
            }
        }

        for (var commandName : BuiltInCommand.values()) {
            executables.add(commandName.name());
        }
        return executables;
    }

}
