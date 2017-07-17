package com.SKAGENSUMMERCAMP;

import me.xdrop.fuzzywuzzy.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by toljoe on 14.07.2017.
 * Sources
 * https://stackoverflow.com/a/32233940 (generate binary numbers)
 * https://stackoverflow.com/a/1816989 (occurrences of character in string)
 */
public class FuzzyNameSearch {
    private CacheFiles cf;
    private HashSet<String> fn;
    private HashSet<String> sn;
    FuzzyNameSearch() {
        this.cf = new CacheFiles();
        this.fn = cf.getFirstName();
        this.sn = cf.getSurName();
    }

    private List<String> generateBinaries(int bits){
        System.out.println("Generating binary list for "+bits+" bits");
        int maxNum = 1 << bits; // max num is 2^16 = 65536
        List<String> bins = new ArrayList<>();
        for (int i = 0; i < maxNum; i++) {
            String bin = Integer.toBinaryString(i);
            while (bin.length() != bits) bin = "0" + bin;
            bins.add(bin);
        }
        return bins;
    }

    private List<String> generatePermutations(String s){
        HashMap<Character, Character> replacementMap = new HashMap<Character, Character>(){{
            put('æ','e'); put('ø','o'); put('å','a'); put('l','i');
        }};
        // Add all unique characters to a new string
        String oddChars = "";
        for (Character c : new char[]{'æ', 'ø', 'å', 'l'}){
            if (StringUtils.contains(s, c) && !StringUtils.contains(oddChars, c)){
                oddChars += c;
            }
        }
        System.out.println("Got these chars: " + oddChars);

        // Get the amount of permutations of the odd characters
        List<String> possibleNames = new ArrayList<>();
        List<String> binaryList = this.generateBinaries(oddChars.length());
        for (String bin : binaryList){
            System.out.println("Creating on/off table for " + bin);
            // Create a copy of oddChars for each permutation
            String tmpOddChars = s;
            for (int i = 0; i < bin.length(); i++){
                Character bit = bin.charAt(i);
                if (bit=='1'){
                    Character newChar = replacementMap.get(oddChars.charAt(i));
                    tmpOddChars = StringUtils.replaceChars(tmpOddChars, oddChars.charAt(i), newChar);
                }
            }
            System.out.println("Done with this permutation, result: "+tmpOddChars);
            possibleNames.add(tmpOddChars);
        }
        return possibleNames;
    }


    private String bestMatch(String name, Collection<String> nameList){
        List<String> possibleMatches = this.generatePermutations(name);
        for (String match : possibleMatches) {
            // do all three possible matches
            System.out.println("Working with " + match);
            System.out.println(FuzzySearch.extractOne(match, nameList));
        }
        // do a hardcoded example of fuzzy replacement
        // as ø and o are similar, attempt to replace it and compare the score
//        if (name.contains("ø")){
//            System.out.println("Contains Ø, checking with it replaced:");
//            System.out.println(FuzzySearch.extractOne(name.replace('ø','o'), nameList));
//        }
//        if (name.contains("å")){
//            System.out.println("Contains Å, checking with it replaced:");
//            System.out.println(FuzzySearch.extractOne(name.replace('å','a'), nameList));
//        }
//        if (name.contains("æ")){
//            System.out.println("Contains Æ, checking with it replaced:");
//            System.out.println(FuzzySearch.extractOne(name.replace('æ','e'), nameList));
//        }


        return "";
//        int _score = -1;
//        String _best = "";
//        for (String n : nl){
//            int fuzzScore = FuzzySearch.ratio(name, n);
//            if (fuzzScore > _score){
//                _score = fuzzScore;
//                _best = n;
//            }
//        }
//        return _best;
    }

    private String compareScore(String orig, String ... fuzzyWords){
        int bestScore = -1;
        String bestWord = "";
        for (String f : fuzzyWords){
            int tmpScore = FuzzySearch.ratio(orig, f);
            // add penalty
            tmpScore -= 10*(Math.abs(orig.length() - f.length()));
            if (tmpScore > bestScore){
                bestScore = tmpScore;
                bestWord = f;
            }
        }
        return bestWord;
    }
//
//    private String compareScore(String original, String fuzz1, String fuzz2){
//        int score1 = FuzzySearch.ratio(original, fuzz1);
//        int score2 = FuzzySearch.ratio(original, fuzz2);
//        // we don't want to strictly compare the two:
//        // - favoring the word of similar length to the original should get prioritized
//        // subtract 10 times the difference in word length
//        int score1penalty = 10*(Math.abs(original.length() - fuzz1.length()));
//        int score2penalty = 10*(Math.abs(original.length() - fuzz2.length()));
//        score1 -= score1penalty;
//        score2 -= score2penalty;
//        String bestMatch = score1>score2?fuzz1:fuzz2;
//        System.out.println("Found " + fuzz1 + "(" + score1 + ") and " + fuzz2 + "(" + score2 + ")");
////        System.out.println("Partial matches: ");
////        System.out.println(FuzzySearch.);
//
//        System.out.println("Best match: "+bestMatch);
////        return score1>score2?fuzz1:fuzz2;
//        return bestMatch;
//    }

    public void handleName(String name){
        name = name.trim().toLowerCase();
        String first = "";
        String sur = "";
        // Keep the first name as the first element, then the rest of the names as the second.
        String[] splitted = name.split("\\s+", 2);
            if (splitted.length > 1) {
                System.out.println("Found some more names");
                first = splitted[0];
                sur = splitted[1];
            }
            else first = name;
        List<String> allNames = new ArrayList<>();
        System.out.println("First name: "+first);
        System.out.println("Middle/surname(s): "+sur);

        String firstNameLetterMatch = this.bestMatch(first, this.cf.getFnLetter(first.charAt(0)));
        String firstNameLengthMatch = this.bestMatch(first, this.cf.getFnLength(first.length()));
        String firstNameAbsoluteMatch = this.bestMatch(first, this.fn);

        String fnBestMatch = this.compareScore(
                first,
                firstNameLengthMatch,
                firstNameLetterMatch,
                firstNameAbsoluteMatch);

        // Compare the entire firstnames file against the name
        allNames.add(fnBestMatch);

        if (sur.length() > 0) {
            for (String s : sur.split("\\s+")) {
                System.out.println("Handling " + s);
                String surNameLetterMatch = this.bestMatch(s, this.cf.getSnLetter(s.charAt(0)));
                String surNameLengthMatch = this.bestMatch(s, this.cf.getSnLength(s.length()));
                String surNameAbsoluteMatch = this.bestMatch(s, this.sn);

                String surNameBestMatch = this.compareScore(
                        s,
                        surNameLengthMatch,
                        surNameLetterMatch,
                        surNameAbsoluteMatch
                );
                allNames.add(surNameBestMatch);
            }
        }
        System.out.println("Parsed name:");
        System.out.println(String.join(" ", allNames));
    }
}
