package com.SKAGENSUMMERCAMP;

import com.github.slugify.Slugify;
import me.xdrop.fuzzywuzzy.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by toljoe on 14.07.2017.
 * Sources
 * https://stackoverflow.com/a/32233940 (generate binary numbers)
 * https://stackoverflow.com/a/1816989 (occurrences of character in string)
 * https://stackoverflow.com/a/38323180 (distinct list)
 *
 *
 * This class heavily uses the Apache Stringutils library
 *  to effectively compute string-based operations
 *
 * @variable triggerChars: ap of characters
 * @variable replaceChars: map of characters to replace @triggerChars with
 * @variable replacementMap: the computed map from above mentioned lists
 * @variable tempCachedWords: an empty Map to store newly found words in
 * @variable cachedWords: the Map to load from cache.
 *                        Dump tempCachedWords into this upon exit.
 * @variable foundLetterMatch: used to verify valid lists
 *                             E.g. the list of names starting with 'Æ' is empty.
 * @variable foundLengthMatch: used to verify names by length
 *                             E.g. the list of names with length 30 is empty
 */

class FuzzyNameSearch {
    private CacheFiles cf = new CacheFiles();
    private char[] triggerChars = new char[]{
            'e', 'o', 'a', 'l', 'n', 'i', '!', 'h', '1', '3', '5', 'q', 'c', 'g'};
    private char[] replaceChars = new char[]{
            'i', 'ø', 'å', 'i', 'r', 'l', 'l', 'i', 'l', 'e', 's', 'å', 'l', 'ø'};
    private HashMap<Character, Character> replacementMap = new HashMap<Character, Character>(){{
        for (int i = 0; i < triggerChars.length; i++) {
            put(triggerChars[i], replaceChars[i]);
        }
    }};
    private HashMap <String, String> tempCachedWords = new HashMap<>();
    private HashMap <String, String> cachedWords =
            (HashMap ) this.cf.getCachedObject("name_matches");
    // Use the cumulative score to sum the absolute value of scores
//    private Map <String, List<Integer>> cumulativeNameScore = new HashMap<>();
    private boolean foundLetterMatch = false;
    private boolean foundLengthMatch = false;
    FuzzyNameSearch(){
    }

    /**
     * Returns a list of all binary numbers up to a certain bit
     * @param: number of bits
     * @return: List with permutations of all binary numbers according to the given bit.
     */
    private List<String> generateBinaries(int bits){
        int maxNum = 1 << bits; // e.g. 4 bits -> 2^4 = 16 permutations
        List<String> bins = new ArrayList<>();
        for (int i = 0; i < maxNum; i++) {
            StringBuilder bin = new StringBuilder(Integer.toBinaryString(i));
            while (bin.length() != bits) bin.insert(0, "0");
            bins.add(bin.toString());
        }
        return bins;
    }

    /**
     * Returns all triggerChars in a string
     * @param s String to find all characters in
     * @return  String
     */
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

    /**
     * Explained below - heavily stiched into "bestMatch"
     */
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

    /**
     * Finds "best match" of a name; computing its Levenshtein score
     * compute the binary permutations of triggerChars within the name
     * use these to generate all permutations of the name, with replaced characters
     *
     * Example:
     *              name = Ådne
     *
     *              find trigger-chars
     *              -> n and e as a string -> "ne"
     *
     *              compute the binary permutations of bit length 2 (length of "ne"):
     *              -> 00, 01, 10, 11
     *              --> where the first bit is "n" and the second bit is "e"
     *              ---> 00: ne, 01: ni, 10: re, 11: ri
     *              ----> this is based on the "triggerChars->replacementChars" mapping
     *
     *              compute permutations of name, based on above explanation
     *              -> Ådne, Ådni, Ådre, Ådri
     *
     *              find the best match of these permutated names using Levenshtein
     *              (the letters do not have to be in order)
     *
     * @param name      the name to be computed
     * @param nameList  the according name list
     * @return          String after handling with "compareScore"
     */
    private String bestMatch(String name, Collection<String> nameList){
        Set<String> matches = new HashSet<>();
        List<String> permutationsOfName = this.generatePermutations(name);
        for (String n : permutationsOfName) {
            // Compute the top three for each possible match, giving a broader sample size.
            Set<String> topThree = new HashSet<>();
            FuzzySearch.extractTop(n, nameList, 3).forEach(
                    s->topThree.add(s.getString()));
            String bestOfThree = this.compareScore(n, topThree);
            // Compare the original name with the best match of three.
            // A fuzzy score of 100 indicates an identical name - return this.
            if (FuzzySearch.ratio(name, bestOfThree) == 100) return bestOfThree;
            // Otherwise, continue adding the best matching names
            matches.add(bestOfThree);
        }
        return this.compareScore(name, matches);
    }

    /**
     * Essence of the program - computes the score for each word
     * Returns the best matching name
     * @param orig          original name passed by program
     * @param fuzzyWords    list of possible fuzzy matches
     * @return String
     */
    private String compareScore(String orig, Set<String> fuzzyWords){
        int bestScore = -999;
        String bestWord = "";
        for (String s : fuzzyWords){
            if (s.length() < 1) continue;
            int tmpScore = FuzzySearch.ratio(orig, s);
            // Add a penalty score of -15 for each letter the names differ in length
            tmpScore -= 15*(Math.abs(orig.length() - s.length()));
            // System.out.println("Comparing " + orig + " to "+ s + ", Score = " + tmpScore);
            // if tmpScore is 100, this name is definitely correct
            if (tmpScore == 100){
                bestWord = s;
                break;
            }
            if (tmpScore > bestScore){
                bestScore = tmpScore;
                bestWord = s;
            }
        }
        // System.out.println(bestWord+" with " + bestScore + " was returned");
        return bestWord;
    }

    /**
     * Return a name, stripping it of weird characters
     * Based on the best scoring one after fuzzy-searching
     *
     * Before doing anything, check if the name is stored in cache,
     *  if so, return it immediately.
     *
     * @param name      a name
     * @param isFirst   Whether to check first name list or not
     * @return String
     */
    private String parseName(String name, boolean isFirst){
        String possibleMatch = this.getFromCache(name);
        if (possibleMatch != null){
            return possibleMatch;
        }

        Set<String> matches = new HashSet<>();
        // attempt to find a name matching on the "by_length" list.
        // there is a chance of no lists occurring, thus the try/catch
        this.foundLetterMatch = false;
        this.foundLengthMatch = false;
        try{
            Collection<String> byLetter = isFirst?
                    this.cf.getFnLetter(name.charAt(0)):
                    this.cf.getSnLetter(name.charAt(0));
            if (byLetter.size() > 2){
                String b = bestMatch(name, byLetter);
                matches.add(b);
                // System.out.println("Validating letter score: " + FuzzySearch.ratio(name,b));
                this.foundLetterMatch = true;
            }
            Collection<String> byLength = isFirst?
                    this.cf.getFnLength(name.length()):
                    this.cf.getSnLength(name.length());
            matches.add(this.bestMatch(name, byLength));
            if (byLength.size() > 2){
                String b = bestMatch(name, byLength);
                matches.add(b);
                //System.out.println("Valdating length score: " + FuzzySearch.ratio(name,b));
                matches.add(bestMatch(name, byLength));
                this.foundLengthMatch = true;
            }
        }
        catch(NullPointerException e){
            //System.out.println("Did not find any list for " + name);
        }
        /*
        Given that we have "Øygard" and "Nygård" as matches for Nygard
        We want to compare the slugs of both these words, and discard the worst

        A "slug" is a way to de-clutter strings.
        */
        Slugify slg = new Slugify();
        String bestSlug = "";
        int bestSlugScore = -999;
        for (String m : matches){
            String slugTxt = slg.slugify(m);
            int tmp = FuzzySearch.ratio(name, slugTxt);
            /*
            The number 10 might need some tweaking.
            It is simply the penalty for length differences.
             */
            tmp -= 10*(Math.abs(name.length() - slugTxt.length()));
//            System.out.println("Comparing " + name + " with the slug of "+m+": " + slugTxt + " scored " + tmp);
            if (tmp>bestSlugScore){
                bestSlugScore = tmp;
                bestSlug = m;
            }
        }
        return bestSlug;
    }

    /**
     * Returns a name based on a split name (e.g. Tor-Gunnar -> [tor, gunnar])
     * Also applies fuzzy-search (e.g. Pqr-Kane -> [per, kåre])
     * @param name              a name
     * @param lookForFirstName  whether to look for a first name or surname.
     * @return
     */
    private List<String> handleSplitName(String name, boolean lookForFirstName){
        List<String> combinedName = new ArrayList<>();
        for (String part : StringUtils.split(name, '-')){
            List<String> partly = handleName(part, lookForFirstName, 0, new ArrayList<>());
            combinedName.addAll(partly);
        }
        return combinedName;
    }

    /**
     * Returns a string, space separated at index
     * @param name
     * @param index
     * @return String
     */
    private String splitNameByIndex(String name, int index){
        String new_part1 = StringUtils.substring(name, 0, index);
        String new_part2 = StringUtils.substring(name, index);
        return StringUtils.joinWith(" ", new_part1, new_part2);
    }


    private List<String> handleName(String name, boolean firstRun,
                                    int index, List<String> complete){
        name = name.trim().toLowerCase();
        List<String> tmpName = new ArrayList<>();
        for (String n : StringUtils.split(name)){
            String best = this.parseName(n, firstRun);
            tmpName.add(best);
            if (n.length() < 13){
                // valid first name
                complete.add(best);
            }
            if (this.foundLetterMatch && !this.foundLengthMatch){
                /*
                 * The length of the presumed first name will be our starting index
                 * The name might not be correct, but it should be a decent indicator
                 */
                int startIdx = tmpName.get(index).length();
                if (tmpName.size() > 1){
                    // there will NEVER be a _SINGLE_ valid name
                    // only add the names if it contains more than one.
                    complete.addAll(tmpName);
                }
                // System.out.println("Using '" + tmpName.get(tmpName.size()-1) + "' to guess the index");
                String new_name = splitNameByIndex(n, startIdx);
                return handleName(new_name, firstRun, tmpName.size()-1, complete);
            }
            firstRun = false;
        }
        return complete.stream().distinct().collect(Collectors.toList());
    }

    private String getFromCache(String n) {
        String key = this.cachedWords.getOrDefault(n, null);
        if (key != null){
            System.out.println("Found " + key + " in cache");
        }
        return key;
    }

    /**
     * The function to be called to search a name
     * E.g. "firstnamelastname" -> ["firstname", "lastname"]
     * @param n     name
     * @return      list of names, result of fuzzy matching
     */
    List<String> searchName(String n){
        n = n.trim().toLowerCase();
        String possibleMatch = getFromCache(n);
        if (possibleMatch != null){
            return Arrays.asList(StringUtils.split(possibleMatch));
        }
        List<String> result;
        /*
        * modify true or false based on what gives the best results generally
        * this is based on whether a search for names starting with surnames is typical
        * (which it's probably not, thus defaulting to true)
         */
        if (n.contains("-")) {
            result = this.handleSplitName(n, true);
        }
        /*
         * A name containing only one name might be only a sur name
         * attempt to solve for both a first name and then a sur name.
         * return the best match.
         * param firstRun: look for first name in the first match.
         */
        else if (StringUtils.split(n).length == 1){
            List<String> startFirst = this.handleName(n, true,
                   0, new ArrayList<>());
            List<String> startSurname = this.handleName(n,
                    false, 0, new ArrayList<>());
            System.out.println("Found two names: " + startFirst.toString() + " and " +
            startSurname.toString());
            int scoreFirst = FuzzySearch.ratio(n, startFirst.get(0));
            int scoreSur = FuzzySearch.ratio(n, startSurname.get(0));
            result = scoreFirst>scoreSur? startFirst : startSurname;
        }
        else{
            List<String> s = new ArrayList<>();
            result = this.handleName(n, true, 0, s);
        }
        System.out.println(n + " => " + result);
        String resultAsString = StringUtils.join(result, " ");
        this.tempCachedWords.put(n, resultAsString);
        System.out.println("writing: " + result + " with key = "+n);
        return result;
    }

    /**
     * Store tempCachedWords in cache (to later be loaded as cachedWords)
     */
    void close(){
        this.cf.cacheNames(this.tempCachedWords);
    }
}
