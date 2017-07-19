package com.SKAGENSUMMERCAMP;
import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        FuzzyNameSearch fs = new FuzzyNameSearch();
        fs.handleName("KRLSTHANFLATIEHM");

//        fs.handleName("Trr-Pøttqr Strndew");
//        Scanner in = new Scanner(System.in);
//        while (in.hasNext()){
//            String name = in.nextLine();
//            System.out.println("found name: " + name);
//            if (name.contains(" ")){
//                fs.handleName(name);
//            }
//            else{
//                fs.simpleHandleName(name);
//            }
//        }
        // comment fs.close() to avoid caching (for debugging)
//         fs.close();
    }
}