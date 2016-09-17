package com.puiyeetong.booklisting;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RetainedBooksFragment booksFragment;

    /** Tag for the log messages */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    /** Maximum number of books to return */
    private static final int MAX_NUM_BOOKS = 10;

    /** URL to query the Google Book dataset for book information */
    private static final String GOOGLE_BOOK_REQUEST_URL =
            "https://www.googleapis.com/books/v1/volumes?maxResults=" + MAX_NUM_BOOKS;

    private static String searchTerms = "";

    private TextView instructionTextView;

    private ArrayList<Book> books;


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (booksFragment != null) {
            books = booksFragment.getBooks();
            updateUi();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instructionTextView = (TextView) findViewById(R.id.instruction_text_view);

        Button searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasInternetConnection()) {
                    EditText searchBox = (EditText) findViewById(R.id.search_box);
                    searchTerms = searchBox.getText().toString().trim();

                    if (searchTerms.length() > 0) {
                        BookAsyncTask task = new BookAsyncTask();
                        task.execute();
                    }
                } else {
                    showInstruction("Please make sure you have internet connection.");
                }
            }
        });
    }

    private void setBooksFragment() {
        // find the retained fragment on activity restarts
        FragmentManager fm = getFragmentManager();
        booksFragment = (RetainedBooksFragment) fm.findFragmentByTag("books");

        // create the fragment and data the first time
        if (booksFragment == null) {
            // add the fragment
            booksFragment = new RetainedBooksFragment();
            fm.beginTransaction().add(booksFragment, "books").commit();
            // load the data from the web
            booksFragment.setBooks(books);
        }
    }

    private boolean hasInternetConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    private void showInstruction(String message) {
        instructionTextView.setText(message);
        instructionTextView.setVisibility(View.VISIBLE);
    }

    private void updateUi() {
        BookAdapter booksAdapter = new BookAdapter(this, books);
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setEmptyView( findViewById(R.id.empty_list_view));
        listView.setAdapter(booksAdapter);
        instructionTextView.setVisibility(View.GONE);
    }

    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI with the first {@MAX_NUM_BOOKS} books in the response.
     */
    private class BookAsyncTask extends AsyncTask<URL, Void, ArrayList> {
        @Override
        protected ArrayList doInBackground(URL... urls) {
            URL url = createUrl(GOOGLE_BOOK_REQUEST_URL + "&q=" + searchTerms);

            String jsonResponse = "";

            try {
                jsonResponse = makeHttpRequest(url);
                books = extractItemsFromJson(jsonResponse);
                setBooksFragment();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return books;
        }

        /**
         * Update the screen with the given book (which was the result of the
         * {@link BookAsyncTask}).
         */
        @Override
        protected void onPostExecute(ArrayList books) {
            updateUi();
        }

        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";

            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;

            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /** milliseconds */);
                urlConnection.setConnectTimeout(15000 /** milliseconds */);
                urlConnection.connect();

                if(urlConnection.getResponseCode() != 200) {
                    throw new IOException("Problem retrieving the book JSON results.");
                }

                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if(inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        /**
         * Return a {@link Book} object by parsing out information
         * about the first {@MAX_NUM_BOOKS} from the input bookJSON string.
         */
        private ArrayList<Book> extractItemsFromJson(String bookJSON) throws JSONException {
            if (TextUtils.isEmpty(bookJSON)) {
                return new ArrayList<Book>();
            }

            JSONObject baseJsonResponse = new JSONObject(bookJSON);
            int totalItems = baseJsonResponse.getInt("totalItems");

            // If there are results in the items array
            if (totalItems > 0) {
                JSONArray itemArray = baseJsonResponse.getJSONArray("items");
                ArrayList<Book> books = new ArrayList<Book>();

                for (int i = 0; i < itemArray.length(); i++) {
                    JSONObject bookItem = itemArray.getJSONObject(i);
                    JSONObject volumeInfo = bookItem.getJSONObject("volumeInfo");

                    String title = volumeInfo.getString("title");
                    String authors = "";
                    if (volumeInfo.isNull("authors")) {
                        authors = "";
                    } else {
                        authors = volumeInfo.getJSONArray("authors").join(", ");
                    }

                    String description;
                    if (volumeInfo.isNull("description")) {
                        description = "";
                    } else {
                        description = volumeInfo.getString("description");
                    }

                    books.add(new Book(title, authors, description));
                }

                return books;
            }

            return new ArrayList<Book>();
        }
    }
}
