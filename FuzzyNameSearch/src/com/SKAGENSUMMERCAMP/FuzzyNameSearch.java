package com.SKAGENSUMMERCAMP;

import com.github.slugify.Slugify;
import com.sun.org.apache.xpath.internal.SourceTree;
import me.xdrop.fuzzywuzzy.*;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by toljoe on 14.07.2017.
 * Sources
 * https://stackoverflow.com/a/32233940 (generate binary numbers)
 * https://stackoverflow.com/a/1816989 (occurrences of character in string)
 */

class FuzzyNameSearch {
    private CacheFiles cf = new CacheFiles();
    // Replace each respective trigger with its replacement
    private char[] triggerChars = new char[]{'e', 'o', 'a', 'l', 'n'};
    private char[] replaceChars = new char[]{'æ', 'ø', 'å', 'i', 'r'};
    private HashMap<Character, Character> replacementMap = new HashMap<Character, Character>(){{
        for (int i = 0; i < triggerChars.length; i++) {
            put(triggerChars[i], replaceChars[i]);
        }
    }};
    @SuppressWarnings("unchecked")
    private HashMap <String, String> tempCachedWords = new HashMap<>();
    private HashMap <String, String> cachedWords =
            (HashMap) this.cf.getCachedObject("name_matches");
    FuzzyNameSearch(){}

    // Returns a list of all binary numbers up to a certain bit
    private List<String> generateBinaries(int bits){
        // System.out.println("Generating binary list for "+bits+" bits");
        int maxNum = 1 << bits; // e.g. 4 bits -> 2^4 = 16 permutations
        List<String> bins = new ArrayList<>();
        for (int i = 0; i < maxNum; i++) {
            StringBuilder bin = new StringBuilder(Integer.toBinaryString(i));
            while (bin.length() != bits) bin.insert(0, "0");
            bins.add(bin.toString());
        }
        return bins;
    }

    private String extractUniqueChars(String s){
        String chars = "";
        for (Character c : this.triggerChars){
            // Check if the parameter string has this char, if so, make sure it's unique
            if (StringUtils.contains(s, c) && !StringUtils.contains(chars, c)){
                chars += c;
            }
        }
        return chars;
    }

    // Returns a list of possible names by replacing certain characters
    private List<String> generatePermutations(String s){
        // Check if the passed name actually has any of the triggers
        // if not, simply return the name (keep Collection type)
        if (!StringUtils.containsAny(s, triggerChars)){
            return Collections.singletonList(s);
        }
        // Add all unique characters to a new string
        String chars = extractUniqueChars(s);
        List<String> possibleNames = new ArrayList<>();
        // Get the amount of permutations of the odd characters
        List<String> binaryList = this.generateBinaries(chars.length());
        for (String bin : binaryList){
            // creating an inner copy of the passed name
            String k = s.intern();
            for (int i = 0; i < bin.length(); i++){
                Character bit = bin.charAt(i);
                if (bit=='1'){
                    Character newChar = replacementMap.get(chars.charAt(i));
                    k = StringUtils.replaceChars(k, chars.charAt(i), newChar);
                }
            }
            // Done with this permutation
            possibleNames.add(k);
        }
        return possibleNames;
    }
    Slugify slg = new Slugify();
    private String bestMatch(String name, Collection<String> nameList){
        Set<String> matches = new HashSet<>();
        List<String> permutationsOfName = this.generatePermutations(name);
        System.out.println("Best match for " + name + " in " + StringUtils.join(permutationsOfName));

        for (String n : permutationsOfName) {
            // System.out.println(n);
            // Compute the top three for each possible match, giving a broader sample size.
            Set<String> topThree = new HashSet<>();
            for (ExtractedResult er : FuzzySearch.extractTop(n, nameList, 3)){
                topThree.add(er.getString());
            }
            String bestOfThree = this.compareScore(n, topThree);
            // A fuzzy score of 100 indicates an identical name - return this.
            if (FuzzySearch.ratio(name, bestOfThree) == 100) return bestOfThree;
            // Otherwise, continue adding the best matching names
            matches.add(bestOfThree);
        }
        // Do some clever calculations to find the actual best match
        return this.compareScore(name, matches);
    }

    private String getCachedKey(String key){
        if (this.cachedWords.containsKey(key))
            return this.cachedWords.get(key);
        else if (this.tempCachedWords.containsKey(key)){
            return this.tempCachedWords.get(key);
        }
        return null;
    }

    // The essence of the program - computes the score for each word
    // Returns the best matching word
    private String compareScore(String orig, Set<String> fuzzyWords){
        // Look up cached words
        int bestScore = -1;
        String bestWord = "";
        for (String s : fuzzyWords){
            if (s.length() < 1) continue;
            // add penalty
            int tmpScore = FuzzySearch.ratio(orig, s);
            // Add a penalty score of -10 for each letter the names differ in length
            tmpScore -= 10*(Math.abs(orig.length() - s.length()));
//            System.out.println("Comparing " + orig + " to "+ s + ", Score = " + tmpScore);
            // if tmpScore is 100, this name is definitely correct
            if (tmpScore == 100){
                System.out.println("Perfect score!");
                bestWord = s;
                break;
            }
            if (tmpScore > bestScore){
                bestScore = tmpScore;
                bestWord = s;
            }
        }
//        System.out.println(bestWord+" with " + bestScore + " was returned");
        return bestWord;
    }

    private String getNameScore(String name, boolean isFirst){
        String cachedKey = getCachedKey(name);
        if (cachedKey != null){
            System.out.println("Found cached key for "+name+": "+cachedKey);
            return cachedKey;
        }

        Set<String> matches = new HashSet<>();
        // attempt to find a name matching on the "by_length" list.
        // there is a chance of no lists occurring, thus the try/catch
        try{
            Collection<String> byLetter = isFirst?
                    this.cf.getFnLetter(name.charAt(0)):
                    this.cf.getSnLetter(name.charAt(0));
            if (byLetter.size() > 0){
                matches.add(bestMatch(name, byLetter));
            }
        }
        catch(NullPointerException e){
            System.out.println("Did not find any list for character " + name.charAt(0));
        }
        // find matches on the "by_length" list
        Collection<String> byLength = isFirst?
                this.cf.getFnLength(name.length()):
                this.cf.getSnLength(name.length());
        matches.add(this.bestMatch(name, byLength));


        System.out.println("Finished fetching name scores");
        matches.forEach(System.out::println);
//        return this.compareScore(name, matches);
        /*
        Given that we have "Øygard" and "Nygård" as matches for Nygard
        We want to compare the slugs of both these words, and discard the worst
         */
        System.out.println("Matches...");
        matches.forEach(System.out::println);
        String bestSlug = "";
        int bestSlugScore = -1;
        for (String m : matches){
            String slugTxt = slg.slugify(m);
            int tmp = FuzzySearch.ratio(name, slugTxt);
            tmp -= 10*(Math.abs(name.length() - slugTxt.length()));
            System.out.println("Comparing " + name + " with the slug of "+m+": " + slugTxt + " scored " + tmp);
            if (tmp>bestSlugScore){
                bestSlugScore = tmp;
                bestSlug = m;
            }
        }
        return bestSlug;
//        System.out.println("BEST FINAL MATCH: " + bestSlug);
//        return this.compareScore(name, sluggedMatches);
    }

    void handleName(String name){
        name = name.trim().toLowerCase();
        String first;
        String sur = "";
        // Keep the first name as the first element, then the rest of the names as the second.
        String[] nameArr = name.split("\\s+", 2);
            if (nameArr.length > 1) {
                first = nameArr[0];
                sur = nameArr[1];
            }
            else first = name;
        System.out.println("First name: "+first);
        System.out.println("Middle/surname(s): "+sur);

        List<String> allNames = new ArrayList<>();
        String best = this.getNameScore(first, true);
        System.out.println("Best word: " + best);
        this.tempCachedWords.put(first, best);
        allNames.add(best);

        if (sur.length() > 0) {
            for (String s : sur.split("\\s+")) {
                System.out.println("Handling " + s);
                best = this.getNameScore(s, false);
                this.tempCachedWords.put(s, best);
                allNames.add(best);

                System.out.println("Best word: " + best);
            }
        }
        System.out.println("Parsed name:");
        System.out.println(String.join(" ", allNames));
    }

    void close(){
        // write all newly found words to cache
        this.cf.cacheObject("name_matches", this.tempCachedWords);
    }
}
