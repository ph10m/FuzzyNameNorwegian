package com.SKAGENSUMMERCAMP;
import com.github.slugify.Slugify;
import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        FuzzyNameSearch fs = new FuzzyNameSearch();
        // Supposed to be Ola Espen Halvor Pedersen Olsen Heidi Lund
        fs.handleName("Haiivard Nygard");
        fs.handleName("Kåle");
//        fs.close();
    }
}

