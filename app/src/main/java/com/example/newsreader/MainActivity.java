package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> titleList = new ArrayList<String>();
    ArrayList<String> urlList = new ArrayList<String>();
    ArrayAdapter adapter;
    SQLiteDatabase newsDB;


    class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {

            StringBuilder result = new StringBuilder();
            URL url;
            HttpURLConnection connection= null;

            try {
                url = new URL(strings[0]);
                connection =(HttpURLConnection) url.openConnection();
                InputStream inStream = (InputStream) connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inStream);
                int data = reader.read();

                while(data != -1){
                    char c = (char) data;
                    result.append(c);
                    data = reader.read();
                }

                JSONObject jsonObject = new JSONObject(result.toString());
                jsonObject = new JSONObject(jsonObject.getString("response"));
                JSONArray jsonArray = new JSONArray(jsonObject.getString("results"));

                newsDB.execSQL("DELETE FROM articles");
                String sql = "INSERT INTO articles (title, url) VALUES (?, ?)";
                SQLiteStatement statement = newsDB.compileStatement(sql);

                for(int i=0; i<jsonArray.length();i++){
                    statement.bindString(1,jsonArray.getJSONObject(i).getString("webTitle"));
                    statement.bindString(2,jsonArray.getJSONObject(i).getString("webUrl"));
                    statement.execute();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

    public void updateListView(){

        Cursor c = newsDB.rawQuery("SELECT * FROM articles",null);

        int titleIndex = c.getColumnIndex("title");
        int urlIndex = c.getColumnIndex("url");

        if(c.moveToFirst()) {

            titleList.clear();
            urlList.clear();

            do {
                /*Log.i("TITLE ", c.getString(titleIndex));
                Log.i("URL ", c.getString(urlIndex));*/
                titleList.add(c.getString(titleIndex));
                urlList.add(c.getString(urlIndex));
            } while(c.moveToNext());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newsDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        newsDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, title VARCHAR, url VARCHAR)");


        listView = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titleList);
        listView.setAdapter(adapter);

        updateListView();

        new DownloadTask().execute("https://content.guardianapis.com/search?page-size=20&q=india&api-key=15876448-65b1-448a-baf7-5b558bf2a3bf");

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("url",urlList.get(position));
                startActivity(intent);
            }
        });

    }
}