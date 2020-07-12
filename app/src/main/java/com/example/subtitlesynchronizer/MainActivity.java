package com.example.subtitlesynchronizer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    EditText timeFromUser;
    TextView doneText, pathText;
    private Uri uri;    //Uri of the subtitle file that was selected by the user
    private String data;    //all the data of subtitle file is put into this string

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeFromUser = findViewById(R.id.time);
        doneText = findViewById(R.id.doneText);
        pathText = findViewById(R.id.pathBox);
    }

    public void openAboutActivity(View view){   //opens 'aboutActivity' when user clicks on
                                                //the question mark button
        Intent intent = new Intent(this, aboutActivity.class);
        startActivity(intent);
    }


    public void main(View view) {
        readText(uri);
        boolean timeMatchFound = false, textMatchFound = false;
        List<String> timeList = new ArrayList<>(); // A list of all the timestamps.
        List<String> textList = new ArrayList<>(); // A list of all the texts in subtitle file

        int numOfSecToBeChanged;
        try{
            numOfSecToBeChanged = Integer.parseInt(timeFromUser.getText().toString());
        } catch(NumberFormatException e){
            e.printStackTrace();
            doneText.setTextSize(14f);
            doneText.setText(R.string.invalidInput);
            return;
        }

        Matcher timeMatcher; //Regex matcher object that fetches all the timestamps
        Matcher textMatcher; ////Regex matcher object that fetches all the text

        try{
            timeMatcher = timeRegex().matcher(data);
            textMatcher = textRegex().matcher(data);
        } catch(NullPointerException e){
            e.getMessage();
            doneText.setTextSize(14f);
            doneText.setText(R.string.chooseFile);
            return;
        }


        while (timeMatcher.find()) {
            timeMatchFound = true;
            int subtitleSec, subtitleMin, subtitleHr, subtitleMilsec;
            subtitleHr = Integer.parseInt(timeMatcher.group(1));
            subtitleMin = Integer.parseInt(timeMatcher.group(3));
            subtitleSec = Integer.parseInt(timeMatcher.group(5));
            subtitleMilsec = Integer.parseInt(timeMatcher.group(7));
            if (numOfSecToBeChanged > 0)
                timeList.add(addTime(numOfSecToBeChanged, subtitleMilsec, subtitleSec, subtitleMin, subtitleHr));
            else {
                if(checkDecreasingTime(numOfSecToBeChanged) == null)
                    return;
                timeList.add(subtractTime(numOfSecToBeChanged, subtitleMilsec, subtitleSec, subtitleMin, subtitleHr));
            }
        }
        while (textMatcher.find()){
            textMatchFound = true;
            textList.add(textMatcher.group(2));
        }

        //When no matches are found for either the timestamps or the texts, mostly the file is in
        //a different format than usual .srt files or the file itself is not a subtitle file
        if (!timeMatchFound || !textMatchFound) {
            System.out.println("ERROR: this subtitle file cannot be edited");
            doneText.setTextSize(14f);
            doneText.setText(R.string.onlySrtFiles);
            return;
        }

        ArrayList<List<String>> arrayOfSegregatedLists;
        arrayOfSegregatedLists = segregateList(timeList);
        List<String> leftList = arrayOfSegregatedLists.get(0);
        List<String> rightList = arrayOfSegregatedLists.get(1);

        alterDocument(uri, leftList, rightList, textList);
        System.out.println("done");
        doneText.setTextSize(30f);
        doneText.setText(R.string.done);
        timeFromUser.setText("");
    }


    public void openDirectory(View view) {
        if(timeFromUser.getText().toString().equals("")){
            System.out.println("ERROR: time not entered when selecting file");
            Toast.makeText(this, "Enter time first!", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "choose .srt file"), 123);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                uri = data.getData();
            }
        }
        pathText.setText(uri.getPath());
        //readText(uri);
    }

    private void readText(Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(NullPointerException e){
            e.getMessage();
            doneText.setTextSize(14f);
            doneText.setText(R.string.chooseFile);
            return;
        }
        data = sb.toString();
    }

    private void alterDocument(Uri uri, List<String> left, List<String> right, List<String> text) {
        try{
            ParcelFileDescriptor pfd = getApplication().getContentResolver().
                                       openFileDescriptor(uri, "w");
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            Iterator leftElement = left.iterator();
            Iterator rightElement = right.iterator();
            Iterator textElement = text.iterator();
            int count = 1;
            while (leftElement.hasNext() && rightElement.hasNext() && textElement.hasNext()){
                fos.write((count + "\n").getBytes());
                fos.write((leftElement.next().toString() + " --> " + rightElement.next().toString()).getBytes());
                fos.write((textElement.next().toString() + "\n\n").getBytes());

                count++;
            }
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //checks that decrease in time requested by user is lesser than
    // the time of the first subtitle entry, returns null otherwise
    String checkDecreasingTime(int numOfSecToBeChanged) {
        numOfSecToBeChanged = Math.abs(numOfSecToBeChanged);
        Pattern p = Pattern.compile("(\\d\\d)(:)(\\d\\d)(:)(\\d\\d)(,)(\\d\\d\\d)");
        Matcher textMatcher = p.matcher(data);
        int subtitleSec = 0, subtitleMin = 0, subtitleHr = 0;

        //finds only the first entry in subtitle file
        if (textMatcher.find()) {
            subtitleHr = Integer.parseInt(textMatcher.group(1));
            subtitleMin = Integer.parseInt(textMatcher.group(3));
            subtitleSec = Integer.parseInt(textMatcher.group(5));
        }
        if (numOfSecToBeChanged > (subtitleHr * 3600 + subtitleMin * 60 + subtitleSec)) {
            System.out.println("ERROR: too much decrease in time.");
            doneText.setTextSize(14f);
            doneText.setText(R.string.tooMuchDecrease);
            return null;
        }
        return "ok";    //return any non-null value if everything is fine
    }

    //divides the timeList into a 'left part' and 'right part' so the modified timestamps
    // and texts are written properly in the new srt file
    static ArrayList<List<String>> segregateList(List<String> listOfTimes){
        ArrayList<List<String>> arrayOfTimeLists = new ArrayList<>();
        int count = 0;
        List<String> segregateLeft = new ArrayList<>(),
                     segregateRight = new ArrayList<>();
        for(String item : listOfTimes){
            if (count % 2 == 0)
                segregateLeft.add(item);
            else
                segregateRight.add(item);
            count += 1;
        }
        arrayOfTimeLists.add(segregateLeft);
        arrayOfTimeLists.add(segregateRight);
        return arrayOfTimeLists;
    }

    static Pattern timeRegex(){
        return Pattern.compile("(\\d\\d)(:)(\\d\\d)(:)(\\d\\d)(,)(\\d\\d\\d)");
    }

    static Pattern textRegex(){
        return Pattern.compile("--> (\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d)([\\S\\s]*?)\\n\\n");
    }

    static String addTime(int numOfSec, int milsec, int sec, int min, int hr) {
        int changedMin, changedHr;
        int changedSec = sec + numOfSec;
        if (changedSec > 60) {
            int rawMinutes = changedSec / 60;
            changedSec = changedSec - rawMinutes * 60;
            changedMin = rawMinutes + min;
            if (changedMin > 60) {
                int rawHours = changedMin / 60;
                changedMin = changedMin - rawHours * 60;
                changedHr = hr + rawHours;
            } else if (changedMin == 60) {
                changedMin = 0;
                changedHr = hr + 1;
            } else {
                changedMin = min + rawMinutes;
                changedHr = hr;
            }
        } else if (changedSec == 60) {
            changedMin = min + 1;
            if (changedMin == 60) {
                changedMin = 0;
                changedHr = hr + 1;
                changedSec = 0;
            } else {
                changedSec = 0;
                changedHr = hr;
            }
        } else {
            changedMin = min;
            changedHr = hr;
        }
        System.out.println("seconds after addition=" + changedSec);
        return String.format(Locale.ENGLISH,"%02d", changedHr) + ":" + String.format(Locale.ENGLISH,"%02d" ,changedMin) + ":" +
                String.format(Locale.ENGLISH,"%02d", changedSec) + "," + String.format(Locale.ENGLISH,"%03d", milsec);
    }

    static String subtractTime(int numOfSec, int milsec, int sec, int min, int hr) {
        int rawSeconds, rawMinutes, rawHours;
        numOfSec = Math.abs(numOfSec);
        if (numOfSec > 3600) {
            rawHours = numOfSec / 3600;
            hr = hr - rawHours;
            rawSeconds = numOfSec - rawHours * 3600;
            if (rawSeconds < 60) {
                if (sec >= rawSeconds)
                    numOfSec = sec - rawSeconds;
                else {
                    min = min - 1;
                    rawSeconds = rawSeconds - sec;
                    numOfSec = 60 - rawSeconds;

                }
            } else if ((60 < rawSeconds) && (rawSeconds < 3600)) {
                rawMinutes = rawSeconds / 60;
                min = min - rawMinutes;
                rawSeconds = rawSeconds - rawMinutes * 60;
                numOfSec = sec - rawSeconds;
            } else if (rawSeconds == 60)
                min += 1;
        } else if ((60 < numOfSec) && (numOfSec < 3600)) {
            rawMinutes = numOfSec / 60;
            min = min - rawMinutes;
            rawSeconds = numOfSec - rawMinutes * 60;
            if (rawSeconds > sec){
                if (min > 0){
                    min--;
                }
                else {
                    min = 59;
                    hr--;
                }
                numOfSec = 60 - (rawSeconds - sec);
            }
            else
                numOfSec = sec - rawSeconds;

        } else if ((0 < numOfSec) && (numOfSec < 60)) {
            if (numOfSec < sec)
                numOfSec = sec - numOfSec;
            else if (numOfSec > sec) {
                if (min == 0) {
                    hr -= 1;
                    min = 59;
                    numOfSec = 60 - (numOfSec - sec);
                }
                else {
                    min = min - 1;
                    rawSeconds = numOfSec - sec;
                    numOfSec = 60 - rawSeconds;
                }
            }
            else        //when numOfSec = sec
                numOfSec = numOfSec - sec;

        } else if (numOfSec == 3600) {
            hr = hr - 1;
            numOfSec = sec;
        } else if (numOfSec == 60) {
            min -= 1;
            numOfSec = sec;
        } else if (numOfSec == 0)
            numOfSec = sec;
        System.out.println("seconds after subtraction=" + numOfSec);
        return String.format(Locale.ENGLISH,"%02d", hr) + ":" + String.format(Locale.ENGLISH,"%02d" ,min) + ":" +
                String.format(Locale.ENGLISH,"%02d", numOfSec) + "," + String.format(Locale.ENGLISH,"%03d", milsec);
    }
}
