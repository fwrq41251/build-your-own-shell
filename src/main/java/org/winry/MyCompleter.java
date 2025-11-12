package org.winry;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.Collections;
import java.util.List;

public class MyCompleter implements Completer {

    private final Trie trie = new Trie();
    private String lastPrefix = null;

    public MyCompleter(List<String> executables) {
        for (String cmd : executables) {
            trie.insert(cmd);
        }
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String currentWord = line.word();

        var allMatches = trie.getWordsWithPrefix(currentWord);

        if (allMatches.isEmpty()) {
            return;
        }

        if (allMatches.size() == 1) {
            candidates.add(new Candidate(allMatches.getFirst()));
        } else {
            String lcp = trie.getLongestCommonPrefix(currentWord);
            if (lcp.length() > currentWord.length()) {
                candidates.add(new Candidate(
                        lcp,    // value to insert
                        lcp,    // value to display
                        null,   // group
                        null,   // description
                        null,   // suffix
                        null,   // key
                        false   // <-- 关键！告诉 JLine 这是一个不完整的补全，不要加空格！
                ));
            } else {
                // Multiple matches but no further common prefix
                if (currentWord.equals(lastPrefix)) {
                    Collections.sort(allMatches);
                    var matchesStr = String.join("  ", allMatches);
                    System.out.println("\n" + matchesStr);
                    reader.callWidget(LineReader.REDRAW_LINE);
                    lastPrefix = null;
                } else {
                    reader.getTerminal().puts(org.jline.utils.InfoCmp.Capability.bell);
                    lastPrefix = currentWord;
                }
            }
        }
    }

}
