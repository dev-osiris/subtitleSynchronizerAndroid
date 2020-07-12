package com.example.subtitlesynchronizer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

public class aboutActivity extends AppCompatActivity{
TextView text;

    public void openMainActivity(View view){   //opens 'MainActivity' when user clicks on
                                                //the back button
         super.onBackPressed();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_about);
        text = findViewById(R.id.aboutText);
        text.setText("Synchronize any .srt subtitle file with this app.\n\n" +
                     "Enter amount of seconds you want to add or subtract.\n" +
                     "Ex: Enter -12 for advancing the subtitles 12 seconds and enter" +
                     " 11 for delaying subtitles 11 seconds.\n\nNote that you" +
                     " cannot decrease time more than the time when the first dialogue" +
                     " was spoken.\n\nCurrently only .srt files are supported.");
    }

}
