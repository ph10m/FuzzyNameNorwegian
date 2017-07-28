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
 */

class FuzzyNameSearch {
    private CacheFiles cf = new CacheFiles();
    // Replace each respective trigger with its replacement
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
    private String bestMatch(String name, Collection<String> nameList){

        Set<String> matches = new HashSet<>();
        List<String> permutationsOfName = this.generatePermutations(name);
//        System.out.println("Best match for " + name + " in " + StringUtils.join(permutationsOfName));

        for (String n : permutationsOfName) {
            // System.out.println(n);
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
        // Do some clever calculations to find the actual best match
        return this.compareScore(name, matches);
    }

    // The essence of the program - computes the score for each word
    // Returns the best matching word
    private String compareScore(String orig, Set<String> fuzzyWords){
        // Look up cached words
        int bestScore = -999;
        String bestWord = "";
        for (String s : fuzzyWords){
            if (s.length() < 1) continue;
            // add penalty
            int tmpScore = FuzzySearch.ratio(orig, s);
            // Add a penalty score of -15 for each letter the names differ in length
            tmpScore -= 15*(Math.abs(orig.length() - s.length()));
//            System.out.println("Comparing " + orig + " to "+ s + ", Score = " + tmpScore);
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
//        System.out.println(bestWord+" with " + bestScore + " was returned");
        return bestWord;
    }

    private String getNameScore(String name, boolean isFirst){
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
//                matches.add(bestMatch(name, byLetter));
//                System.out.println("Validating letter score: " + FuzzySearch.ratio(name,b));
                this.foundLetterMatch = true;
            }
            Collection<String> byLength = isFirst?
                    this.cf.getFnLength(name.length()):
                    this.cf.getSnLength(name.length());
            matches.add(this.bestMatch(name, byLength));
            if (byLength.size() > 2){
                String b = bestMatch(name, byLength);
                matches.add(b);
//                System.out.println("Valdating length score: " + FuzzySearch.ratio(name,b));
                matches.add(bestMatch(name, byLength));
                this.foundLengthMatch = true;
            }
        }
        catch(NullPointerException e){
//            System.out.println("Did not find any list for " + name);
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
            tmp -= 10*(Math.abs(name.length() - slugTxt.length()));
//            System.out.println("Comparing " + name + " with the slug of "+m+": " + slugTxt + " scored " + tmp);
            if (tmp>bestSlugScore){
                bestSlugScore = tmp;
                bestSlug = m;
            }
        }

        return bestSlug;
    }

    private List<String> handleSplitName(String name, boolean lookForFirstName){
        List<String> combinedName = new ArrayList<>();
        for (String part : StringUtils.split(name, '-')){
            List<String> partly = altHandleName(part, lookForFirstName, 0, new ArrayList<>());
            combinedName.addAll(partly);
        }
        return combinedName;
    }

    private String splitNameByIndex(String name, int index){
//        System.out.println("Using index " + index);
        String new_part1 = StringUtils.substring(name, 0, index);
        String new_part2 = StringUtils.substring(name, index);
        return StringUtils.joinWith(" ", new_part1, new_part2);
    }

    private List<String> altHandleName(String name, boolean firstRun,
                                       int index, List<String> complete){
        name = name.trim().toLowerCase();
//        System.out.println("First? " + firstRun);
        List<String> tmpName = new ArrayList<>();
        for (String n : StringUtils.split(name)){
            String best = this.getNameScore(n, firstRun);
            tmpName.add(best);
            if (n.length() < 13){
                // valid first name
                complete.add(best);
            }
//            }
//            System.out.println("Found letter list? " + this.foundLetterMatch);
//            System.out.println("Found length list? " + this.foundLengthMatch);
            if (this.foundLetterMatch && !this.foundLengthMatch){
                // The length of the presumed first name will be our starting index
                // The name might not be correct, but it should be a decent indicator
//                System.out.println("###############TEMPNAMES: " + tmpName.toString());
//                System.out.println("Index: " + index);
                int startIdx = tmpName.get(index).length();
                if (tmpName.size() > 1){
                    // if this outer if-statement is true,
                    // there will NEVER be a _SINGLE_ valid name
                    // only add the names if it contains more than one name.
//                    tmpName.remove(lastAdded);
                    complete.addAll(tmpName);
                }
//                System.out.println("Using '" + tmpName.get(tmpName.size()-1) + "' to guess the index");
                String new_name = splitNameByIndex(n, startIdx);
//                tmpName.add(new_name);
//                System.out.println("New name: " + new_name);
//                System.out.println("Handling the entire name recursively!");
//                System.out.println("PASSING ON THE INDEX --------> " + Integer.toString(tmpName.size()-1));
                return altHandleName(new_name, firstRun, tmpName.size()-1, complete);
            }
            firstRun = false;
        }
        return complete.stream().distinct().collect(Collectors.toList());
    }

    private List<String> handleName(String name){
        name = name.trim().toLowerCase();
        String possibleMatch = getFromCache(name);
        if (possibleMatch != null){
            return Arrays.asList(StringUtils.split(possibleMatch));
        }
        String first;
        // Keep the first name as the first element, then the rest of the names as the second.
        String[] nameArr = StringUtils.split(name, " ", 2);
            if (nameArr.length > 1) {
                first = nameArr[0];
            }
            else first = name;
//        System.out.println("First name: "+first);
//        System.out.println("Middle/surname(s): "+sur);
        List<String> allNames = new ArrayList<>();

        if (first.contains("-")){
            allNames.addAll(handleSplitName(name, true));
        }
        else{
            String best = this.getNameScore(first, true);
            allNames.add(best);
        }
        return allNames;
    }

    private String getFromCache(String n) {
        String key = this.cachedWords.getOrDefault(n, null);
        if (key != null){
            System.out.println("Found " + key + " in cache");
        }
        return key;
    }

    List<String> checkName(String n){
        n = n.trim().toLowerCase();
        String possibleMatch = getFromCache(n);
        if (possibleMatch != null){
            return Arrays.asList(StringUtils.split(possibleMatch));
        }

        List<String> result;
        if (n.contains("-")) {
//            System.out.println("Handling a split name..");
            result = this.handleName(n);
        }
        else if (StringUtils.split(n).length == 1){
            // A name containing only one name might be only a sur name
            // attempt to solve for both a first name and then a sur name.
            // return the best match.
            // param firstRun: look for first name in the first match.
            List<String> startFirst = this.altHandleName(n, true,
                   0, new ArrayList<>());
            List<String> startSurname = this.altHandleName(n,
                    false, 0, new ArrayList<>());
            System.out.println("Found two names: " + startFirst.toString() + " and " +
            startSurname.toString());
            int scoreFirst = FuzzySearch.ratio(n, startFirst.get(0));
            int scoreSur = FuzzySearch.ratio(n, startSurname.get(0));
            result = scoreFirst>scoreSur? startFirst : startSurname;
        }
        else{
            List<String> s = new ArrayList<>();
//            return this.altHandleName(n,
//                    true, 0, s);
            result = this.altHandleName(n, true, 0, s);
        }
        System.out.println(n + " => " + result);
        String resultAsString = StringUtils.join(result, " ");
        this.tempCachedWords.put(n, resultAsString);
        System.out.println("writing: " + result + " with key = "+n);
        return result;
    }
    void close(){
        // write all newly found words to cache
        this.cf.cacheNames(this.tempCachedWords);
    }
}
