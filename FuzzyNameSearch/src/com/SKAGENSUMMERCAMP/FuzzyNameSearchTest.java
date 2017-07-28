package com.SKAGENSUMMERCAMP;


import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Created by toljoe on 19.07.2017.
 */
public class FuzzyNameSearchTest {
    private FuzzyNameSearch fs = new FuzzyNameSearch();
    // delete cache file to ensure everything is checked from scratch
//    File name_matches = new File("src/data/name_matches.cache");

    @Test
    public void fuzzy_SingleName(){
        compare("andreas","adreans");
        compare("odd", "qdd");
        compare("hallvard", "hailward");
        compare("jørgensen", "jørgensen");
        compare("bodil", "Bodrl");
        compare("gabriel", "Gabrlel");
    }

    @Test
    public void fuzzy_HyphenName(){
        compare("tor petter", "Trr-Pøttqr");
        compare("kjell per", "Kjlle-Pør");
        compare("arne kato", "anre-katx");
        compare("odd petter", "odd-petter");
        compare("kjell odd", "kjell-odd");
    }

    @Test
    public void fuzzy_HyphenAndSurname(){
        compare("per gunnar pedersen", "Pqr-Gwnnar Federsen");
    }

    @Test
    public void fuzzy_HyphenSurname(){
        compare("marit kathrine oscar stein johannessen",
                "Mariw-Kothnire Ozcan-Sthen Jotnhasnzen");
    }

    @Test
    public void fuzzy_CombinedFirstLastName(){
        compare("kristian flatheim","KRLSTHANFLATIEHM");
        compare("eirik ulversøy","eLrikulversou");
    }

    @Test
    public void fuzzy_RegularNameFormat(){
        compare("tollef jørgensen","toilef jqrgænsne");
        compare("gudbrand andreas tandberg","gobdradn adreans tnadberg");
        compare("knut gabriel thorstensen", "knqt gabnlel thorstinsen");
        compare("yvonne vervik", "Yvonne Vervrk");
        compare("bodil lande", "BOdIl Lande");
        compare("ingrid voll eltun", "ingrld voll ettun");
        compare("nina øyre", "n1na øyree");
        compare("andrea voll eltun", "andraa voll etlun");
        compare("eirik heggø", "ehirik hoggø");
    }

    private void compare(String expected, String input){
        Assert.assertEquals(toList(expected), fs.checkName(input));
    }

    private List<String> toList(String s){
        return Arrays.asList(StringUtils.split(s));
    }
}
