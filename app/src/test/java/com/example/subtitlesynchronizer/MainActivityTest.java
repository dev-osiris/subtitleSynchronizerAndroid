package com.example.subtitlesynchronizer;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class MainActivityTest{

    @Test
    public void addTime(){
        int milsec1 = 100, sec1 = 8, min1 = 3, hr1 = 1;
        String expectedRes =
        String.format(Locale.ENGLISH,"%02d", hr1) + ":" + String.format(Locale.ENGLISH,"%02d" ,min1) + ":" +
        String.format(Locale.ENGLISH,"%02d", sec1) + "," + String.format(Locale.ENGLISH,"%03d", milsec1);

        assertEquals(expectedRes, MainActivity.addTime(10, 100, 58, 2, 1));
    }

    @Test
    public void subtractTime(){
        int milsec1 = 100, sec1 = 53, min1 = 1, hr1 = 1;
        String expectedRes =
        String.format(Locale.ENGLISH,"%02d", hr1) + ":" + String.format(Locale.ENGLISH,"%02d" ,min1) + ":" +
        String.format(Locale.ENGLISH,"%02d", sec1) + "," + String.format(Locale.ENGLISH,"%03d", milsec1);

        assertEquals(expectedRes, MainActivity.subtractTime(10, 100, 3, 2, 1));
    }
}