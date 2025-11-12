package org.winry;

import java.util.ArrayList;
import java.util.List;

public class Trie {

    private TrieNode root = new TrieNode(new ArrayList<>(), false, '\0');

    public Trie() {

    }

    public void insert(String word) {
        var currentNode = root;
        for (char c : word.toCharArray()) {
            var childNode = containsChild(currentNode, c);
            if (childNode != null) {
                currentNode = childNode;
            } else {
                var newNode = new TrieNode(new ArrayList<>(), false, c);
                currentNode.children.add(newNode);
                currentNode = newNode;
            }
        }
        currentNode.isEndOfWord = true;
    }

    private TrieNode containsChild(TrieNode node, char c) {
        for (TrieNode child : node.children) {
            if (child.value == c) {
                return child;
            }
        }
        return null;
    }

    public boolean search(String word) {
        var currentNode = root;
        var result = true;
        for (char c : word.toCharArray()) {
            var childNode = containsChild(currentNode, c);
            if (childNode == null) {
                result = false;
                break;
            } else {
                currentNode = childNode;
            }
        }
        return result && currentNode.isEndOfWord;
    }

    public boolean startsWith(String prefix) {
        var currentNode = root;
        var result = true;
        for (char c : prefix.toCharArray()) {
            var childNode = containsChild(currentNode, c);
            if (childNode == null) {
                result = false;
                break;
            } else {
                currentNode = childNode;
            }
        }
        return result;
    }

    public List<String> getWordsWithPrefix(String prefix) {
        var currentNode = root;
        List<String> results = new ArrayList<>();
        for (char c : prefix.toCharArray()) {
            var childNode = containsChild(currentNode, c);
            if (childNode == null) {
                return results; // No words with the given prefix
            } else {
                currentNode = childNode;
            }
        }
        collectWords(currentNode, new StringBuilder(prefix), results);
        return results;
    }

    public String getLongestCommonPrefix(String prefix) {
        var currentNode = root;
        StringBuilder commonPrefix = new StringBuilder();
        for (char c : prefix.toCharArray()) {
            var childNode = containsChild(currentNode, c);
            if (childNode == null) {
                break; // No further common prefix
            } else {
                commonPrefix.append(c);
                currentNode = childNode;
            }
        }
        
        if (currentNode == root) {
            return ""; // No common prefix found
        }

        while (currentNode.children.size() == 1 && !currentNode.isEndOfWord) {
            currentNode = currentNode.children.getFirst();
            commonPrefix.append(currentNode.value);
        }
        return commonPrefix.toString();
    }

    private void collectWords(TrieNode node, StringBuilder prefix, List<String> results) {
        if (node.isEndOfWord) {
            results.add(prefix.toString());
        }
        for (TrieNode child : node.children) {
            prefix.append(child.value);
            collectWords(child, prefix, results);
            prefix.deleteCharAt(prefix.length() - 1); // Backtrack
        }
    }

    public static void main(String[] args) {
        var trie = new Trie();
        trie.insert("apple");
        System.out.println(trie.search("apple"));   // true
        System.out.println(trie.search("app"));     // false
        System.out.println(trie.startsWith("app")); // true
        trie.insert("app");
        System.out.println(trie.search("app"));     // true
        System.out.println(trie.getWordsWithPrefix("ap")); // [apple, app]
        System.out.println(trie.getLongestCommonPrefix("bce")); // ap


        trie.insert("xyz_foo");
        trie.insert("xyz_foo_bar");
        trie.insert("xyz_foo_bar_baz");
        System.out.println(trie.getLongestCommonPrefix("xyz_")); // [xyz_foo, xyz_foo_bar,
        // xyz_foo_bar_baz]
        System.out.println(trie.getLongestCommonPrefix("xyz_foo_")); // [xyz_foo_bar_baz]
    }

    private static class TrieNode {
        List<TrieNode> children;
        boolean isEndOfWord;
        char value;

        TrieNode(List<TrieNode> children, boolean isEndOfWord, char value) {
            this.children = children;
            this.isEndOfWord = isEndOfWord;
            this.value = value;
        }
    }

}
