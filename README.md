[![progress-banner](https://backend.codecrafters.io/progress/shell/af9d1d9e-3cc3-4424-8e40-d9750e3be557)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

# Build Your Own Shell

This project is a custom POSIX-compliant shell built in Java. It provides a feature-rich
command-line interface capable of executing built-in commands, running external programs, and
handling complex shell operations like pipelines and I/O redirection.

## Overview

This shell is built as part of the "Build Your Own Shell" challenge from CodeCrafters. It leverages
the `jline` library to provide a robust and interactive REPL experience, including tab completion
and command history.

## Features

* **REPL:** An interactive Read-Eval-Print Loop for entering and executing commands.
* **Built-in Commands:**
    * `exit [status]`: Exit the shell with an optional status code.
    * `echo [message]`: Print a message to the console.
    * `pwd`: Print the current working directory.
    * `cd [path]`: Change the current directory. Supports `~` for the home directory.
    * `type [command]`: Determine if a command is a shell built-in or an external program.
    * `history`: View and manage command history with flags for reading (`-r`), writing (`-w`), and
      appending (`-a`) to a history file.
* **External Command Execution:** Find and execute programs from the `PATH` environment variable.
* **Pipelines (`|`):** Chain multiple commands together, where the output of one command becomes the
  input of the next. The shell correctly handles pipelines that mix built-in and external commands.
* **I/O Redirection:**
    * `>` / `1>`: Redirect standard output to a file.
    * `>>` / `1>>`: Append standard output to a file.
    * `2>`: Redirect standard error to a file.
    * `2>>`: Append standard error to a file.
* **Command Parsing:** A robust parser that correctly handles:
    * Arguments with spaces.
    * Single (`'`) and double (`"`) quotes.
    * Escape characters (`\`).
* **Tab Completion:**
    * Press `Tab` to auto-complete built-in and external commands.
    * If multiple commands match, it completes to the longest common prefix.
    * Press `Tab` again to see a list of all possible completions.
* **Command History:**
    * Loads command history from a file specified by the `HISTFILE` environment variable.
    * Saves new commands to the history file when the shell exits.

## Building and Running

This project uses Maven for building. A convenience script is provided to handle the build and
execution process.

1. **Prerequisites:** Ensure you have `mvn` (Maven) and a Java Development Kit (JDK) installed.
2. **Run the shell:**
   ```sh
   ./your_program.sh
   ```
   This script will compile the Java source code and launch the shell.

## Usage

### Basic Commands

You can run both built-in commands and any executable available in your system's `PATH`.

```sh
$ echo "Hello, World!"
Hello, World!
$ ls -l
... (output of ls) ...
```

### Pipelines

Chain commands together using the `|` operator.

```sh
$ history | grep cd
```

### I/O Redirection

Redirect output and errors to files.

```sh
# Write "Hello" to output.txt
$ echo "Hello" > output.txt

# Append "World" to output.txt
$ echo "World" >> output.txt

# Redirect errors to error.log
$ cat non_existent_file 2> error.log
```