package com.puiyeetong.booklisting;

import android.app.Fragment;
import android.os.Bundle;

import java.util.ArrayList;

public class RetainedBooksFragment extends Fragment {

    // books we want to retain
    private ArrayList<Book> books;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public ArrayList<Book> getBooks() {
        return books;
    }

    public void setBooks(ArrayList<Book> books) {
        this.books = books;
    }
}
