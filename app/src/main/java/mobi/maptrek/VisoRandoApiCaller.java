package mobi.maptrek;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VisoRandoApiCaller extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... urls) {
        try {
            String visoRandoUrl = "https://www.visorando.com/en/index.php?component=user&task=redirectToContent&from=gpxRando&idRandonnee=4098306";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(visoRandoUrl)
                    .get()
                    .build();
            try {
                Response response = client.newCall(request).execute();
                String php =  response.body().string();
                int indexClickHere = php.indexOf("click here");
                String line = php.substring(indexClickHere - 150, indexClickHere);
                int indexHref = line.indexOf("href");
                String gpxUrl = line.substring(indexHref + 6, line.length() - 2);

                Request requestGpx = new Request.Builder()
                        .url(gpxUrl)
                        .get()
                        .build();
                Response responseGpx = client.newCall(requestGpx).execute();
                String gpx = responseGpx.body().string();
                return gpx;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            this.exception = e;

            return null;
        } finally {
        }
        return null;
    }

    protected void onPostExecute(String response) {
        Log.d("POST EXECUTE VISORANDO",response);
    }
}