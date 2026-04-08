package edu.hitsz.aircraftwar.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 分数数据访问实现（使用SQLite替代CSV文件）
 */
public class ScoreDaoImpl extends SQLiteOpenHelper implements ScoreDao {

    private static final String DB_NAME = "aircraft_war.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "scores";

    public ScoreDaoImpl(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "score INTEGER NOT NULL, " +
                "play_time TEXT NOT NULL, " +
                "difficulty TEXT NOT NULL DEFAULT 'EASY')";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void doAdd(Score score) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", score.getName());
        values.put("score", score.getScore());
        values.put("play_time", score.getPlayTime());
        values.put("difficulty", score.getDifficulty());
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    @Override
    public List<Score> getAllScores() {
        List<Score> scores = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, "score DESC");
        while (cursor.moveToNext()) {
            Score score = new Score(
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("score")),
                    cursor.getString(cursor.getColumnIndexOrThrow("play_time")),
                    cursor.getString(cursor.getColumnIndexOrThrow("difficulty"))
            );
            score.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            scores.add(score);
        }
        cursor.close();
        db.close();
        return scores;
    }

    @Override
    public List<Score> getScoresByDifficulty(String difficulty) {
        List<Score> scores = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "difficulty = ?",
                new String[]{difficulty}, null, null, "score DESC");
        while (cursor.moveToNext()) {
            Score score = new Score(
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("score")),
                    cursor.getString(cursor.getColumnIndexOrThrow("play_time")),
                    cursor.getString(cursor.getColumnIndexOrThrow("difficulty"))
            );
            score.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            scores.add(score);
        }
        cursor.close();
        db.close();
        return scores;
    }

    @Override
    public void deleteScore(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
