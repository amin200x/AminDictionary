package com.amin.amindictionary;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.support.v7.widget.Toolbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

 public  class  MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

   private ListView definitionListview;
   private ArrayList<String> definitions;
   private ArrayList<String> selectedWords;
   private ArrayList<String> englishToFarsi;
   private ArrayList<String> farsi;
   private ArrayList<String> englishToEnglish;
   private ArrayAdapter<String> defintionAdapter;
   private ArrayAdapter<String> wordsAdapter;
   private AutoCompleteTextView autoWordEditText;
   private SQLiteDatabase myDictionary;
   private DatabaseHelper mDBHelper;
   private Spinner dropDownList ;
   private InputMethodManager mgr;
   private TextToSpeech textToSpeech;
   Menu menu;
   private ClipboardManager clipboardManager;
     LinearLayout contentLinearLayout;
     LinearLayout loadingLinearLayout;

    private class LoadDataRunnable implements Runnable{
        private String columnName;
        private String query;
        private List<String> words;

        LoadDataRunnable(String columnName, String query, List<String> words){
            this.columnName = columnName;
            this.query = query;
            this.words = words;
        }
        @Override
        public void run() {
            try {

                mDBHelper=new DatabaseHelper(MainActivity.this);
                mDBHelper.updateDataBase();
                myDictionary = mDBHelper.getReadableDatabase();
                Cursor c = myDictionary.rawQuery(query, null);
                int columnIndex = c.getColumnIndex(columnName);
                words.clear();
                if (c.moveToFirst())
                {
                    do {

                        String word = "";
                        word = c.getString(columnIndex);
                        words.add(word);
                    } while (c.moveToNext());

                }
            runOnUiThread(()->{
                words.remove(null);
                words.remove("");
                autoWordEditText.setAdapter(wordsAdapter);
                wordsAdapter.notifyDataSetChanged();
                myDictionary.close();
                loadingLinearLayout.setVisibility(View.GONE);
            });
            } catch(Exception e){
            runOnUiThread(()->{
                Toast.makeText(MainActivity.this, "اشکال در برنامه! برنامه را بسته و دوبار اجرا کنید!!", Toast.LENGTH_LONG).show();
            });
            }
        }
    }
    private   void findWord(View view, String word) {

        String column2Name = "definition";
        String column3Name = null;
        String query = "";
        definitionListview.setAdapter(null);
        if ( dropDownList.getSelectedItemPosition() == 0 )
        {
            column3Name = null;
            query = "SELECT Distinct definition FROM Farsi INNER JOIN Words ON Farsi.id = Words.id WHERE word = ? ORDER BY definition";

        }
        else if (dropDownList.getSelectedItemPosition() == 1)
        {
            column2Name = "word";
            column3Name = null;
            query = "SELECT Distinct word FROM Words INNER JOIN Farsi ON Farsi.id = Words.id WHERE definition = ? ORDER BY word";
        }
        else if (dropDownList.getSelectedItemPosition() == 2)
        {
            column3Name = "type";
            query = "SELECT type, definition FROM English INNER JOIN Words ON English.id = Words.id WHERE word = ? ";
        }
        mgr.hideSoftInputFromWindow(autoWordEditText.getWindowToken(), 0);

        try {
                mDBHelper=new DatabaseHelper(this);
                mDBHelper.updateDataBase();
                myDictionary = mDBHelper.getReadableDatabase();
                 word = word.trim().toLowerCase();
                Cursor c = myDictionary.rawQuery(query, new String[]{word});
                int columnNameIndex2 = -1;
                int columnNameIndex3 = -1;
                if (column2Name != null) {
                    columnNameIndex2 = c.getColumnIndex(column2Name);
                }
                if (column3Name != null) {
                    columnNameIndex3 = c.getColumnIndex(column3Name);
                }
                definitions.clear();
                if (c.moveToFirst())
                {
                    do {
                        String wordtypeDefinition = "";
                        if (columnNameIndex3 != -1)
                        {
                            wordtypeDefinition = c.getString(columnNameIndex3) + "\n";
                            wordtypeDefinition += c.getString(columnNameIndex2);
                            definitions.add(wordtypeDefinition);
                        }
                        else
                       {
                            wordtypeDefinition = c.getString(columnNameIndex2);
                            definitions.remove(wordtypeDefinition);
                            definitions.add(wordtypeDefinition);
                        }
                    } while (c.moveToNext());
                }
                definitionListview.setAdapter(defintionAdapter);
                defintionAdapter.notifyDataSetChanged();
                myDictionary.close();
        } catch (Exception e) {

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        autoWordEditText =  findViewById(R.id.autoWordTextView);
        definitionListview = findViewById(R.id.defintionListview);
        definitions = new ArrayList<>();
        englishToFarsi = new ArrayList<>();
        selectedWords = new ArrayList<>();
        farsi = new ArrayList<>();
        englishToEnglish = new ArrayList<>();
        dropDownList = findViewById(R.id.spinner);
        contentLinearLayout = findViewById(R.id.linearLayoutContent);
        loadingLinearLayout = findViewById(R.id.linearLayoutLoading);
        mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        textToSpeech = new TextToSpeech(this,this);
        loadingLinearLayout.setVisibility(View.GONE);

        String[] dropDownsItems = new String[] { "انگلیسی به فارسی",  "فارسی به انگلیسی", "انگلیسی به انگلیسی"};
        ArrayAdapter<String> dropDownAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, dropDownsItems);
        dropDownList.setAdapter(dropDownAdapter);
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onPrimaryClipChanged() {
                        if(clipboardManager == null)
                            System.out.println("Clipboard is null");
                        String clipBoardText = clipboardManager.getPrimaryClip().getItemAt(0).coerceToText(getBaseContext()).toString();

                        String definition = "";
                        if (!clipBoardText.isEmpty()) {
                            findWord(null, clipBoardText);
                        }
                        if (definitions.size() > 0) {
                            for (String item : definitions) {
                                definition += item + ", ";
                            }
                        } else {
                            definition = "در دیکشنری  پیدا نشد!";
                        }
                        NotificationManager notificationManager = null;
                        Notification notify = null;
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivities(getApplicationContext(), 1, new Intent[]{intent}, 0);
                        if (notificationManager == null) {
                            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        }
                        String CHANNEL_ID = "AminDict";
                        CharSequence CHANNEL_NAME = "Amin Dictionary";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // create android channel
                            NotificationChannel AminDictChannel = new NotificationChannel(CHANNEL_ID,
                                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                            // Sets whether notifications posted to this channel should display notification lights
                            AminDictChannel.enableLights(true);
                            // Sets whether notification posted to this channel should vibrate.
                            AminDictChannel.enableVibration(true);
                            // Sets the notification light color for notifications posted to this channel
                            AminDictChannel.setLightColor(Color.GREEN);
                            // Sets whether notifications posted to this channel appear on the lockscreen or not
                            AminDictChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                            notificationManager.createNotificationChannel(AminDictChannel);

                            notify = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                    .setContentTitle("معنی لغت: " + clipBoardText)
                                    .setStyle(new Notification.BigTextStyle().bigText(definition))
                                    .setContentText(definition)
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent)
                                    .setSmallIcon(android.R.drawable.ic_dialog_info).build();

                            notificationManager.notify(1, notify);

                        } else {
                            notify = new Notification.Builder(getApplicationContext())
                                    .setContentTitle("معنی لغت: " + clipBoardText)
                                    .setStyle(new Notification.BigTextStyle().bigText(definition))
                                    .setContentText(definition)
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent)
                                    .setSmallIcon(android.R.drawable.ic_dialog_info).build();

                            notificationManager.notify(1, notify);
                        }

                    }
                });

        defintionAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, definitions)

        {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                view.setBackgroundColor(Color.rgb(180,230,230));
                if (position % 2 == 1) {
                    // Set a background color for ListView regular row/item
                    view.setBackgroundColor(Color.rgb(150,220,190));
                }
                return view;
            }
        };
        autoWordEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!autoWordEditText.getText().toString().isEmpty()) {
                    findWord(view, autoWordEditText.getText().toString());
                } else {
                    Toast.makeText(getApplicationContext(), "کلمه ای برای جستجو وارد نشده است!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        autoWordEditText.setOnEditorActionListener(new AutoCompleteTextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    autoWordEditText.clearFocus();
                    if (!autoWordEditText.getText().toString().isEmpty()) {
                        findWord(v, autoWordEditText.getText().toString());
                    } else {
                        Toast.makeText(getApplicationContext(), "کلمه ای برای جستجو وارد نشده است!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
        definitionListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if( dropDownList.getSelectedItemPosition() != 0)
                {
                    textToSpeech.setPitch((float) 1);
                    textToSpeech.setSpeechRate((float) 0.75);
                    String text = definitions.get(position);
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }

            }
        });

        dropDownList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                definitionListview.setAdapter(null);
                autoWordEditText.setAdapter(null);
                autoWordEditText.setText("");
                loadingLinearLayout.setVisibility(View.VISIBLE);

                String query;
                mgr.hideSoftInputFromWindow(autoWordEditText.getWindowToken(), 1);
                // if dropdownlist item is english-to-english select words that id's of English table are equal to Words table id's
                if (position == 2) {
                    wordsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, englishToEnglish)

                    {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            view.setBackgroundColor(Color.rgb(180,230,230));
                            if (position % 2 == 1) {
                                // Set a background color for ListView regular row/item
                                view.setBackgroundColor(Color.rgb(150,220,190));
                            }
                            return view;
                        }
                    };

                    if (englishToEnglish.size() > 0) {
                        loadingLinearLayout.setVisibility(View.GONE);
                        autoWordEditText.setAdapter(wordsAdapter);
                        wordsAdapter.notifyDataSetChanged();
                    }else
                    {
                        query = "SELECT DISTINCT word FROM Words INNER JOIN English on Words.id = English.id ORDER BY word asc";
                        new Thread(new LoadDataRunnable("word", query, englishToEnglish)).start();
                        //autoCompleteWords("word", query, englishToEnglish);
                         }
                }
                // if dropdownlist item is english-to-Farsi select words that id's of Farsi table are equal to Words table id's
                else if (position == 0) {
                    wordsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, englishToFarsi)
                    {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            view.setBackgroundColor(Color.rgb(180,230,230));
                            if (position % 2 == 1) {
                                // Set a background color for ListView regular row/item
                                view.setBackgroundColor(Color.rgb(150,220,190));
                            }
                            return view;
                        }
                    };

                    if (englishToFarsi.size() > 0)
                    {
                        loadingLinearLayout.setVisibility(View.GONE);
                        autoWordEditText.setAdapter(wordsAdapter);
                        wordsAdapter.notifyDataSetChanged();
                }else
                    {
                        query = "SELECT DISTINCT word FROM Words INNER JOIN Farsi on Words.id = Farsi.id ORDER BY word asc";
                        new Thread(new LoadDataRunnable("word", query, englishToFarsi)).start();
                        //autoCompleteWords("word", query, englishToFarsi);
                       }
            }
                // if english-to english or english-to-farsi selected do below if condition
                if (position == 0 || position == 2)
                {
                    autoWordEditText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    findViewById(R.id.linearLayoutContent).setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    autoWordEditText.setHint("Enter a word!");

                }

                // if farsi-to-english  selected do below if condition and get all words of Farsi table
                if (position == 1)

                {
                    autoWordEditText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    findViewById(R.id.linearLayoutContent).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    definitionListview.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    wordsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, farsi)
                    {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                            View view = super.getView(position, convertView, parent);
                            view.setBackgroundColor(Color.rgb(180,230,230));
                            if (position % 2 == 1) {
                                // Set a background color for ListView regular row/item
                                view.setBackgroundColor(Color.rgb(150,220,190));
                            }
                            return view;
                        }
                    };
                    if (farsi.size() > 0)
                    {
                       loadingLinearLayout.setVisibility(View.GONE);
                       autoWordEditText.setAdapter(wordsAdapter);
                       wordsAdapter.notifyDataSetChanged();
                    }else
                    {
                        query = "SELECT DISTINCT definition FROM Farsi ORDER BY definition DESC ";
                        new Thread(new LoadDataRunnable("definition", query, farsi)).start();
                       // autoCompleteWords( "definition", query, farsi);
                    }
                    autoWordEditText.setHint("کلمه ای را تایپ کن!");
                }


            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

     //close function for our app
    private  void closeApplication()
    {
        // show dialog to be assured to exit from app
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(" ")
                .setMessage("از برنامه خارج می شوید!؟")
                .setPositiveButton("بله", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("خیر",null).create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id)
        {
            case R.id.action_exit:
            {
                closeApplication();
                break;
            }
            case R.id.action_pronunce:
            {

                if(!autoWordEditText.getText().toString().isEmpty())
                    speakOut();
            break;
            }
            case R.id.action_about:{
                showAboutDialog();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

     public void showAboutDialog(){
         AlertDialog.Builder aboutDialog = new  AlertDialog.Builder(this);
         TextView contentTextView = new TextView(this);
         ImageView imageView = new ImageView(this);
         LinearLayout linearLayout = new LinearLayout(this);
         linearLayout.setOrientation(LinearLayout.VERTICAL);
         linearLayout.setGravity(Gravity.CENTER);
         imageView.setImageResource(R.drawable.pete);
         imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
         contentTextView.setTextSize( 20f);
         contentTextView.setGravity(Gravity.CENTER);
         contentTextView.setText("    The app is desinged by A.Ghadimian, in 2018.");
         linearLayout.addView(imageView);
         linearLayout.addView(contentTextView);
         aboutDialog.setView(linearLayout)

                 .setTitle("About AminDictionary")
                 .setCancelable(false)
                 .setPositiveButton("OK", (dialog,  which) -> dialog.dismiss()).show();


     }
    @Override
    protected void onDestroy() {

        if (textToSpeech != null)
        {
           textToSpeech.stop();
            textToSpeech.shutdown();

        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            }

        }
    }

    private void speakOut() {

        textToSpeech.setPitch((float) 1);
        textToSpeech.setSpeechRate((float) 0.75);
        // the app have to be assured that selected option is not Farsi-to-English since app cant pronunce Farsi words
        // (entered word in textbox is Farsi)
        if( !autoWordEditText.getText().toString().isEmpty()
                && dropDownList.getSelectedItemPosition() != 1)
        {
            String text = autoWordEditText.getText().toString();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

}
