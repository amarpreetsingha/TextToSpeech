package com.aceshub.tts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.LocaleDisplayNames;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.RunnableFuture;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    EditText editText;
    Button button;
    TextToSpeech textToSpeech;
    int FILE_SELECT_CODE = 1;
    Spinner localesSpinner;
    ArrayAdapter<String> localesAdapter;
    List<String> localeList = new ArrayList<>();
    Locale[] locales = Locale.getAvailableLocales();
    Context context;
    int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);
        button = (Button) findViewById(R.id.button);
        localesSpinner = (Spinner) findViewById(R.id.locales);
        context = getApplicationContext();

        initTextToSpeech();

        localesSpinner.setOnItemSelectedListener(this);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = editText.getText().toString();
                if (toSpeak != "") {
                    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    Toast.makeText(MainActivity.this, "Enter something", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
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

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.importFile:
                getFile();
                return true;

            case R.id.exportFile:
                exportFile();
                return true;

            default:
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
        if (requestCode == FILE_SELECT_CODE) {
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
        if (stringBuilder != null)
            editText.setText(stringBuilder);
        else
            Toast.makeText(this, "File empty", Toast.LENGTH_SHORT).show();
    }

    public void initTextToSpeech() {

        textToSpeech = new TextToSpeech(getApplicationContext(), new OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    button.setEnabled(true);
                    if(count++ == 0)
                        setAdapter();
                } else if (status == TextToSpeech.ERROR) {
                    Toast.makeText(context, "Initialisation failed", Toast.LENGTH_SHORT).show();
                    button.setEnabled(false);
                    Log.d("TTS", "Failed");
                }
            }
        });
    }


    private static Map<String, Locale> displayNames = new HashMap<String, Locale>();
    static {
        for (Locale l : Locale.getAvailableLocales()) {
            displayNames.put(l.getDisplayName(), l);
        }
    }

    public static Locale getLocale(String displayName) {
        return displayNames.get(displayName);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ((TextView)view).setTextColor(Color.BLACK);
        int result = textToSpeech.setLanguage(getLocale(parent.getItemAtPosition(position).toString()));
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED ) {
            // Lanuage data is missing or the language is not supported.
            Toast.makeText(context, "Language data is not installed", Toast.LENGTH_SHORT).show();
        }else
            Toast.makeText(context, parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void setAdapter() {
        localeList.clear();
        for (Locale locale : locales) {
            int res = textToSpeech.isLanguageAvailable(locale);
            if (res == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                localeList.add(locale.getDisplayName());
            }
        }
        localesAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, localeList);
        localesSpinner.setAdapter(localesAdapter);

    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void exportFile() {

        final String destFile;
        HashMap<String, String> myHashRender = new HashMap<String, String>();
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, editText.getText().toString());
        String exStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d("MainActivity", "exStoragePath : "+exStoragePath);
        File appTmpPath = new File(exStoragePath + "/sounds/");
        boolean isDirectoryCreated = appTmpPath.mkdirs();
        Log.d("MainActivity", "directory "+appTmpPath+" is created : "+isDirectoryCreated);
        String tempFilename = "audio.wav";
        destFile = appTmpPath.getAbsolutePath() + File.separator + tempFilename;
        Log.d("MainActivity", "tempDestFile : "+destFile);

        textToSpeech.synthesizeToFile(editText.getText().toString(), myHashRender, destFile);

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Exporting to file", Toast.LENGTH_SHORT).show();
                    }
                }));

            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "File saved in " + destFile, Toast.LENGTH_SHORT).show();
                    }
                }));
            }

            @Override
            public void onError(String utteranceId) {
//                Toast.makeText(context, "Error in exporting", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

