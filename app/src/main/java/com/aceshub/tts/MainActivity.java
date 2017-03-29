 package com.aceshub.tts;

import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.zip.Inflater;

 public class MainActivity extends AppCompatActivity {

    EditText editText;
    Button button;
    TextToSpeech textToSpeech;
    int FILE_SELECT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);
        button = (Button) findViewById(R.id.button);

        initTextToSpeech();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = editText.getText().toString(); 
                if(toSpeak != "") {
                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }else{
                    Toast.makeText(MainActivity.this, "Enter something", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

     @Override
     protected void onResume() {
         super.onResume();
         initTextToSpeech();
     }

     @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.importFile :
                getFile();
                return true;

            default :
                return super.onOptionsItemSelected(item);
        }
    }

    public void getFile() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         if(requestCode == FILE_SELECT_CODE) {
             Uri uri = data.getData();
             BufferedReader bufferedReader;
             StringBuilder stringBuilder = new StringBuilder();
             try {
                 bufferedReader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
                 String line;
                 while ((line = bufferedReader.readLine()) != null) {
                     stringBuilder.append(line);
                 }
                 bufferedReader.close();
                 setTextForSpeech(stringBuilder);
             } catch (FileNotFoundException e) {
                 e.printStackTrace();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
     }

     public void setTextForSpeech(StringBuilder stringBuilder) {
         if(stringBuilder != null)
            editText.setText(stringBuilder);
         else
             Toast.makeText(this, "File empty", Toast.LENGTH_SHORT).show();
     }

     public void initTextToSpeech() {
         textToSpeech = new TextToSpeech(getApplicationContext(), new OnInitListener() {
             @Override
             public void onInit(int status) {
                 if(status == TextToSpeech.SUCCESS) {
                     textToSpeech.setLanguage(Locale.getDefault());
                 }else if(status == TextToSpeech.ERROR) {
                     Log.d("TTS", "Failed");
                 }
             }
         });
     }
 }

