package com.example.isabe.popularmovies;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.example.isabe.popularmovies.adapters.MovieAdapter;
import com.example.isabe.popularmovies.data.MovieContract;
import com.example.isabe.popularmovies.data.MovieDbHelper;
import com.example.isabe.popularmovies.objects.Movie;
import com.example.isabe.popularmovies.objects.MovieList;
import com.example.isabe.popularmovies.utilities.MovieAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.example.isabe.popularmovies.utilities.NetworkUtils.apiKey;

public class MainActivityFragment extends Fragment {

    public static final int LOADER_ID = 11;
    private static final int LOADER_CURSOR_ID = 14;
    static final String BASE_URL = "http://api.themoviedb.org/3/movie/";

    public static final String DEFAULT_POPULAR_MOVIE_DB_URL = "http://api.themoviedb.org/3/movie/popular?";
    public static final String MOVIE_DB_URL_TOP_RATED = "http://api.themoviedb.org/3/movie/top_rated?";
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    public static String movieDisplayStyleLink = DEFAULT_POPULAR_MOVIE_DB_URL;
    private MovieAdapter mMovieAdapter;
    private Movie mMovie;
    private MovieList mMovieList = new MovieList();
    private List<Movie> movieList = new ArrayList<Movie>();
    public int menuSelectionId;
    MenuItem menuItem;
    private Context mContext;
    private final static String MENU_CHOSEN = "selected";
    // set up the movie controller from  Retrofit

    private String[] FAVORITES_PROJECTION = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.DB_MOVIE_ID,
            MovieContract.MovieEntry.DB_TITLE,
            MovieContract.MovieEntry.DB_BACKDROP_PATH,
            MovieContract.MovieEntry.DB_POSTER_PATH,
            MovieContract.MovieEntry.DB_SYNOPSIS,
            MovieContract.MovieEntry.DB_RELEASE_DATE,
            MovieContract.MovieEntry.DB_VOTE_AVERAGE
    };

    private GridView gridView;
    private View rootView;

    private android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> mLoaderCursor =
            new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
                    return new CursorLoader(getActivity(),
                            MovieContract.MovieEntry.CONTENT_URI,
                            FAVORITES_PROJECTION,
                            null,
                            null,
                            null);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                    mMovieAdapter.clear();
                    mMovieAdapter.swapCursor(cursor);
                    MovieDbHelper mOpenMoviesHelper = new MovieDbHelper(getActivity());

                    cursor = getActivity().getContentResolver().query(MovieContract.MovieEntry.CONTENT_URI,
                            FAVORITES_PROJECTION, null, null, null);
                    int primaryKeyColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry._ID);
                    int idColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_MOVIE_ID);
                    int titleColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_TITLE);
                    int posterColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_POSTER_PATH);
                    int backdropColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_BACKDROP_PATH);
                    int releasedDateColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_RELEASE_DATE);
                    int synopsisColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_SYNOPSIS);
                    int voteColumnIndex = cursor.getColumnIndex(MovieContract.MovieEntry.DB_VOTE_AVERAGE);

                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            int primaryKeyID = cursor.getInt(primaryKeyColumnIndex);
                            int currentID = cursor.getInt(idColumnIndex);
                            String currentTitle = cursor.getString(titleColumnIndex);
                            String currentPosterPath = cursor.getString(posterColumnIndex);
                            String currentBackdropPath = cursor.getString(backdropColumnIndex);
                            String currentReleaseDate = cursor.getString(releasedDateColumnIndex);
                            String currentSynopsis = cursor.getString(synopsisColumnIndex);
                            String currentVoteAverage = cursor.getString(voteColumnIndex);

                            mMovie = new Movie(currentTitle, currentReleaseDate, currentSynopsis, currentPosterPath,
                                    currentVoteAverage, currentBackdropPath, currentID);

                            movieList.add(mMovie);
                            //mMovieAdapter.addAll(movieList);
                        }
                    }
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    mMovieAdapter.swapCursor(null);
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            movieList = savedInstanceState.getParcelableArrayList("MOVIE_DETAILS");
        }
    }

    public MainActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("MOVIE_DETAILS", (ArrayList<? extends Parcelable>) movieList);
        outState.putInt(MENU_CHOSEN, menuSelectionId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        start();
        gridView = rootView.findViewById(R.id.movies_grid);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {

                //MovieList thisMovieList = mMovieAdapter.getItem(item);
                Movie thisMovie = mMovieAdapter.getItem(item);

                assert thisMovie != null;
                String originalTitle = thisMovie.getmOriginalTitle();
                String releaseDate = thisMovie.getmReleaseDate();
                String voteAverage = thisMovie.getmVoteAverage();
                String moviePoster = thisMovie.getmImageThumbnail();
                String movieBackdrop = thisMovie.getmBackdropPath();
                String movieSynopsis = thisMovie.getmOverviewMovie();
                int movieID = thisMovie.getmMovieTMDBId();

                Intent showDetailsIntent = new Intent(getActivity(), DetailsActivity.class);

                showDetailsIntent.putExtra("MOVIE_DETAILS", thisMovie);
                startActivity(showDetailsIntent);

            }
        });

        android.support.v4.app.LoaderManager loaderManager = getLoaderManager();
        if (savedInstanceState != null) {
            menuSelectionId = savedInstanceState.getInt(MENU_CHOSEN);
            movieList = savedInstanceState.getParcelableArrayList("MOVIE_DETAILS");
            switch (menuSelectionId) {
                case R.id.top_rated:
                    startTopRated();
                    break;
                case R.id.favorites_id:
                    Log.i(LOG_TAG, "Menu selection id is " + menuSelectionId);
                    getActivity().getSupportLoaderManager().initLoader(LOADER_CURSOR_ID, null, mLoaderCursor);
                    break;
                default:
                    start();
            }
        } else {
            start();

            Log.i(LOG_TAG, "Movie controller is started for popular movies.");
        }
        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            movieList = savedInstanceState.getParcelableArrayList("MOVIE_DETAILS");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "MainActivityFragment onResume");
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        switch (menuSelectionId) {
            case R.id.most_popular:
                menuItem = (MenuItem) menu.findItem(R.id.most_popular);
                menuItem.setChecked(true);
                start();
                break;
            case R.id.top_rated:
                startTopRated();
                menuItem = (MenuItem) menu.findItem(R.id.top_rated);
                menuItem.setChecked(true);
                Log.i(LOG_TAG, "Top rated selection saved");
                break;

            case R.id.favorites_id:
                menuItem = (MenuItem) menu.findItem(R.id.favorites_id);
                menuItem.setChecked(true);
                Log.i(LOG_TAG, "Favorites selection saved.");
                break;

        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            /**case R.id.action_settings:
             Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
             startActivity(settingsIntent);
             return true;
             **/
            case R.id.top_rated:
                startTopRated();
                menuSelectionId = id;
                Log.e(LOG_TAG, getString(R.string.log_top_rated_menu) + id);
                break;
            case R.id.most_popular:
                start();
                menuSelectionId = id;
                break;
            case R.id.favorites_id:
                getActivity().getSupportLoaderManager().restartLoader(LOADER_CURSOR_ID, null, mLoaderCursor);
                menuSelectionId = id;
                Log.e(LOG_TAG, getString(R.string.favorites_chosen) + id);
                break;
            case R.id.delete_all:
                deleteData();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteData() {
        int numDeleted = getActivity().getContentResolver()
                .delete(MovieContract.MovieEntry.CONTENT_URI, null, null);
        Toast.makeText(getContext(), "Database deleted: " + numDeleted + " items.", Toast.LENGTH_LONG).show();

    }

    //excerpt from MovieController to test

    public void start(){
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MovieAPI movieAPI = retrofit.create(MovieAPI.class);

        Call<MovieList> callPopular = movieAPI.loadPopularMovies(apiKey);
        //Call<List<Movie>> callTopRate = movieAPI.loadPopularMovies("top_rated");
        callPopular.enqueue(new Callback<MovieList>() {
            @Override
            public void onResponse(Call<MovieList> call, Response<MovieList> response) {
                Log.i(LOG_TAG, "Popular movies call is started." + BASE_URL + "popular?api_key=" + apiKey);
                mMovieList = response.body();
                assert mMovieList != null;
                movieList = mMovieList.getMovieList();
                mMovieAdapter = new MovieAdapter(getActivity().getApplicationContext(), movieList);
                gridView.setAdapter(mMovieAdapter);
            }

            @Override
            public void onFailure(Call<MovieList> call, Throwable t) {
                t.printStackTrace();
            }
        });
        //callTopRate.enqueue((retrofit2.Callback<List<Movie>>) this);
    }

    public void startTopRated(){
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MovieAPI movieAPI = retrofit.create(MovieAPI.class);

        Call<MovieList> callTopRate = movieAPI.loadTopRatedMovies(apiKey);
        callTopRate.enqueue(new Callback<MovieList>() {
            @Override
            public void onResponse(Call<MovieList> call, Response<MovieList> response) {
                Log.i(LOG_TAG, "Top rated movies call is started." + BASE_URL + "top_rated?api_key=" + apiKey);
                mMovieList = response.body();
                assert mMovieList != null;
                movieList = mMovieList.getMovieList();
                mMovieAdapter = new MovieAdapter(getActivity().getApplicationContext(), movieList);
                gridView.setAdapter(mMovieAdapter);
            }

            @Override
            public void onFailure(Call<MovieList> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }


}

