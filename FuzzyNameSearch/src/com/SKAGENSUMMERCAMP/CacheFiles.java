package com.SKAGENSUMMERCAMP;

/**
 * Created by toljoe on 14.07.2017.
 * Sources:
 * https://stackoverflow.com/a/3019388 (Adding list to hashmap)
 * http://beginnersbook.com/2013/12/how-to-serialize-hashmap-in-java/ (Serializing hashmap)
 */

import java.io.*;
import java.util.*;

class CacheFiles {
    /**
     * Create needed files and do logic tests
     */
    CacheFiles(){
        if (!new File("src/data/name_matches.cache").exists()){
            cacheObject("name_matches", new HashMap<String, List<String>>());
        }
        List<String> cachedFiles = Arrays.asList(
                "by_length_firstnames.cache",
                "by_letter_firstnames.cache",
                "by_length_surnames.cache",
                "by_letter_surnames.cache");
        for (String c : cachedFiles){
            if (!new File("src/data/"+c).exists()){
                System.out.println("Missing cache, rebuilding...");
                rebuildCache();
            }
        }
    }

    Object getCachedObject(String fileName) {
        /**
         * Returns a cached object from filename
         * This object must be casted later (e.g. to HashMap)
         * @param: name of file, path is handled later.
         * @return: object found in file (any type)
         */
        Object cachedFile = null;
        fileName = "src/data/"+fileName+".cache";
        // System.out.println("Returning cached file: "+fileName);
        try {
            FileInputStream fs = new FileInputStream(fileName);
            ObjectInputStream os = new ObjectInputStream(fs);
            cachedFile = os.readObject();
            os.close();
            fs.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load " + fileName +", the file might be missing. Rebuild.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Cache file corrupted. Rebuild");
        }
        return cachedFile;
    }
    Collection<String> getFnLetter(Character identifier){
        /**
         * Returns a list of first names based on its first letter
         * @param: first character of a name
         * @return: list of names matching @param
         */
        //noinspection unchecked
        HashMap<Character, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_letter_firstnames");
        return tmp.get(identifier);
    }

    Collection<String> getFnLength(int identifier){
        /**
         * Returns a list of first names based on its length
         * @param: length of a name
         * @return: list of names matching @param
         */
        //noinspection unchecked
        HashMap<Integer, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_length_firstnames");
        return tmp.get(identifier);
    }

    Collection<String> getSnLetter(Character identifier){
        /**
         * Returns a list of surnames based on its first letter
         * @param: first character of a name
         * @return: list of names matching @param
         */
        //noinspection unchecked
        HashMap<Character, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_letter_surnames");
        return tmp.get(identifier);
    }

    Collection<String> getSnLength(int identifier){
        /**
         * Returns a list of surnames based on its length
         * @param: length of a name
         * @return: list of names matching @param
         */
        //noinspection unchecked
        HashMap<Integer, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_length_surnames");
        return tmp.get(identifier);
    }

    private void cacheObject(String fileName, Object obj){
        fileName = "src/data/"+fileName+".cache";
//        System.out.println("Caching file: "+fileName);
        try {
            FileOutputStream fs = new FileOutputStream(fileName);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(obj);
            os.close();
            fs.close();
//            System.out.println(fileName + " cached successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void cacheNames(HashMap<String, String> hm){
        /**
         * Takes in a HashMap and cache it
         * @param hm: a HashMap to be cached
         */
        String fileName = "name_matches";
//        System.out.println("Rewriting name match cache");
        HashMap <String, String> tmp = (HashMap) getCachedObject(fileName);
        tmp.putAll(hm);
        this.cacheObject(fileName, tmp);
    }

    private void rebuildCache(){
        /**
         * Builds cache from the listed files
         * TODO: store these online
         */
        File firstNames = new File("src/data/firstnames.txt");
        File surNames = new File("src/data/surnames.txt");
        List<String> nameFiles = Arrays.asList("firstnames", "surnames");
        if (firstNames.exists() && surNames.exists()){
            System.out.println("Found required files");
            for (String nameFile : nameFiles){
                String filePath = "src/data/" + nameFile + ".txt";
                HashMap<Character, Collection<String>> byNameLetter = new HashMap<>();
                HashMap<Integer, Collection<String>> byNameLength = new HashMap<>();
                HashSet<String> allNames = new HashSet<>();
                // populate allNames from the two text files
                try {
                    BufferedReader br = new BufferedReader(new FileReader(filePath));
                    String line;
                    while ((line = br.readLine()) != null){
                        allNames.add(line.toLowerCase());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // if the map has no field for the list to be added, create it.
                // Otherwise, add to the list.
                for (String n : allNames){
                    byNameLetter.computeIfAbsent(n.charAt(0), x -> new ArrayList<>()).add(n);
                    byNameLength.computeIfAbsent(n.length(), x -> new ArrayList<>()).add(n);
                }
                // store as *.cache files
                cacheObject("by_length_"+nameFile, byNameLength);
                cacheObject("by_letter_"+nameFile, byNameLetter);
            }
        }
        else{
            System.out.println("Couldn't find text files used to build cache");
        }
    }



}
