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
    CacheFiles(){
        // Check if "name_matches.cache" is stored at first:
        if (!new File("src/data/name_matches.cache").exists()){
            cacheObject("name_matches", new HashMap<String, String>());
        }
        List<String> cachedFiles = Arrays.asList("by_length_firstnames.cache",
                "by_letter_firstnames.cache", "by_length_surnames.cache", "by_letter_surnames.cache");
        for (String c : cachedFiles){
            if (!new File("src/data/"+c).exists()){
                System.out.println("Missing cache, rebuilding...");
                rebuildCache();
            }
        }
        // System.out.println("Ready to return cached files");
    }

    Object getCachedObject(String fileName) {
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
        HashMap<Character, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_letter_firstnames");
        return tmp.get(identifier);
    }

    Collection<String> getFnLength(int identifier){
        HashMap<Integer, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_length_firstnames");
        return tmp.get(identifier);
    }

    Collection<String> getSnLetter(Character identifier){
        HashMap<Character, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_letter_surnames");
        return tmp.get(identifier);
    }

    Collection<String> getSnLength(int identifier){
        HashMap<Integer, Collection<String>> tmp =
                (HashMap) this.getCachedObject("by_length_surnames");
        return tmp.get(identifier);
    }
//    HashMap getFnLetter(){
//        return (HashMap) this.getCachedObject("by_letter_firstnames");
//    }
//    HashMap getFnLength(){
//        return (HashMap) this.getCachedObject("by_length_firstnames");
//    }
//    HashMap getSnLetter(){
//        return (HashMap) this.getCachedObject("by_letter_surnames");
//    }
//    HashMap getSnLength(){
//        return (HashMap) this.getCachedObject("by_length_surnames");
//    }
//    HashSet<String> getFirstName(){
//        return (HashSet<String>) this.getCachedObject("raw_firstnames");
//    }
//    HashSet<String> getSurName(){
//        return (HashSet<String>) this.getCachedObject("raw_surnames");
//    }


    protected void cacheObject(String fileName, Object obj){
        fileName = "src/data/"+fileName+".cache";
        System.out.println("Caching file: "+fileName);
        try {
            FileOutputStream fs = new FileOutputStream(fileName);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(obj);
            os.close();
            fs.close();
            System.out.println(fileName + " cached successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rebuildCache(){
        File firstNames = new File("src/data/firstnames.txt");
        File surNames = new File("src/data/surnames.txt");
        List<String> nameFiles = Arrays.asList("firstnames", "surnames");
        if (firstNames.exists() && surNames.exists()){
            System.out.println("Found firstnames.txt and surnames.txt");
            // Later check if cache file exists
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
//                        System.out.println(line);
                        allNames.add(line.toLowerCase());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // cacheObject("raw_" + nameFile, allNames);
                // if the map has no field for the list to be added, create it. Otherwise, add to the list.
                for (String n : allNames){
                    byNameLetter.computeIfAbsent(n.charAt(0), x -> new ArrayList<>()).add(n);
                    byNameLength.computeIfAbsent(n.length(), x -> new ArrayList<>()).add(n);
                }
//                 Serialize and store to xNameByLength.cache and xNameByLetter.cache respectively
                cacheObject("by_length_"+nameFile, byNameLength);
                cacheObject("by_letter_"+nameFile, byNameLetter);
//                byNameLetter.forEach((k,v) -> System.out.println(k + ": "+ v));
//                byNameLength.forEach((k,v) -> System.out.println(k + ": "+ v));

            }
        }
        else{
            // TODO: store the firstnames.txt and surnames.txt online and read from a GET-interface.
            System.out.println("Couldn't find text files used to build cache");
        }
    }



}
