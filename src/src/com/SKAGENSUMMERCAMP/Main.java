package com.SKAGENSUMMERCAMP;

public class Main {

    public static void main(String[] args) {
        FuzzyNameSearch fs = new FuzzyNameSearch();
        fs.searchName("toccef jqrgænsne");
        // comment fs.close() to avoid caching (for debugging)
        fs.close();
    }
}