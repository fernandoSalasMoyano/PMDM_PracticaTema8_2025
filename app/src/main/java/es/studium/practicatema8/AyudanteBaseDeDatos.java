package es.studium.practicatema8;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class AyudanteBaseDeDatos extends SQLiteOpenHelper
{

    private static final String NOMBRE_BD = "pasitos";
    private static final int VERSION_BD = 1;


    public AyudanteBaseDeDatos(Context contexto)
    {
        super(contexto, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String createTable = "CREATE TABLE datosUbicacion (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "latitud REAL," +
                "longitud REAL," +
                "bateria INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS datosUbicacion");
        onCreate(db);
    }

    public void insertarUbicacion(double latitud, double longitud, int bateria) {
        String insertQuery = "INSERT INTO datosUbicacion (latitud, longitud, bateria) VALUES (" +
                latitud + ", " + longitud + ", " + bateria + ")";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(insertQuery);
    }
    public Cursor obtenerUbicaciones() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM datosUbicacion", null);
    }
}
