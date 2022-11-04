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
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("Articles1", MODE_PRIVATE, null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles1 (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR,  url VARCHAR)");



        DownloadTask task = new DownloadTask();
        try {

            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {

        }

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", urls.get(i));

                startActivity(intent);
            }
        });

        updateListView();
    }

    public void updateListView() {
        //till here all the titles and urls associated with those title.are stored in the 'articles1' table of 'articlesDB'.
        //now when we query then we will deal with downloaded titles and urls.
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles1", null);

        int urlIndex = c.getColumnIndex("url");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {//removing from database tables
            titles.clear();
            urls.clear();

            do {

                titles.add(c.getString(titleIndex));//adding to arraylists.
               urls.add(c.getString(urlIndex));//adding to arraylists.

            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);//these jason things are of result that comes out after processing the API url.
                // also no need to create a json object because json thing is already in array form
                int numberOfItems = 20;

                if (jsonArray.length() < 5) {
                    numberOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles1");

                for (int i=0;i < numberOfItems; i++) {
                    String articleId = jsonArray.getString(i);//first 20 ids are chosen of the jsonarray ,and those ids are converted to string for further use.
                    Log.i("INTERESTING",articleId);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String articleInfo = "";

                    while (data != -1) {
                        char current = (char) data;
                        articleInfo += current;
                        data = inputStreamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");//get information of key 'title'.
                        String articleUrl = jsonObject.getString("url");
                        Log.i("Title and URL",articleTitle+articleUrl);

//                        url = new URL(articleUrl);
//                        urlConnection = (HttpURLConnection) url.openConnection();
//                        inputStream = urlConnection.getInputStream();
//                        inputStreamReader = new InputStreamReader(inputStream);
//                        data = inputStreamReader.read();
//                        String articleContent = "";
//                        while (data != -1) {
//                            char current = (char) data;
//                            articleContent += current;
//                            data = inputStreamReader.read();
//                        }

//                        Log.i("HTML", articleContent);

                        String sql = "INSERT INTO articles1 (articleId, title, url) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
//                        statement.bindString(3,articleContent);
                        statement.bindString(3,articleUrl);

                        statement.execute();
                    }
                }

                Log.i("URL Content", result);
                return result;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();//do this when "do in background is finished".
        }
    }
}