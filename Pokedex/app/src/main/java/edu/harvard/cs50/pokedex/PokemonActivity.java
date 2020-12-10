package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private String url;
    private RequestQueue requestQueue;
    private Button statusButton;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ImageView imageView;
    private TextView descriptionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        statusButton = findViewById(R.id.pokemon_button);
        imageView = findViewById(R.id.pokemon_sprite);
        descriptionTextView = findViewById(R.id.pokemon_description);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();

        load();
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final Integer pokemonId = response.getInt("id");
                    String url1 = "https://pokeapi.co/api/v2/pokemon-species/"+ pokemonId + "/";

                    final JsonObjectRequest request1 = new JsonObjectRequest(Request.Method.GET, url1, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray flavourEntries = response.getJSONArray("flavor_text_entries");
                                for (int i = 0; i < flavourEntries.length(); i++){
                                    JSONObject flavorEntry = flavourEntries.getJSONObject(i);

                                    String lang = flavorEntry.getJSONObject("language").getString("name");

                                    if(lang.equals("en")){
                                        String flavorText = flavorEntry.getString("flavor_text");
                                        descriptionTextView.setText(flavorText);
                                        break;
                                    }

                                }

                            }
                            catch (JSONException e) {
                                Log.e("cs50", "Json error", e);
                            }
                        }
                    },
                            new Response.ErrorListener(){
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e("cs50", "flavorText list error", error);
                                }
                    });
                    requestQueue.add(request1);


                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));

                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        }
                        else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                        if (sharedPreferences.contains(nameTextView.getText().toString())){
                            statusButton.setText("Release");
                        }
                    }

                    JSONObject spriteEntries = response.getJSONObject("sprites");
                    String spriteUrl = spriteEntries.getString("front_default");
                    new DownloadSpriteTask().execute(spriteUrl);

                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });

        requestQueue.add(request);
    }

    public void toggleCatch(View view) {
        String name = nameTextView.getText().toString();
        //gotta catch em all
        if (statusButton.getText().toString().toLowerCase().equals("Catch".toLowerCase())){
            editor.putString(name, name);
            editor.apply();
            editor.commit();
            statusButton.setText("Release");

        }else if (statusButton.getText().toString().toLowerCase().equals("Release".toLowerCase())){
            editor.remove(name);
            editor.apply();
            editor.commit();
            statusButton.setText("Catch");
        }
    }
    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap>{

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            }
            catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }
}


