package creativeLab.samsung.mbf.Utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

public class json {
    public static String AssetJSONFile(String filename, Context context) throws IOException {
        AssetManager manager = context.getAssets();
        InputStream file = manager.open(filename);
        byte[] formArray = new byte[file.available()];
        file.read(formArray);
        file.close();
        return new String(formArray);
    }
}
