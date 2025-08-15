package com.anssy.znewspro.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

public class SharedPreferenceUtils {
    public static final String FILE_NAME = "shunxingass";

    public static String getString(Context context, String key) {
        return context.getSharedPreferences(FILE_NAME, 0).getString(key, "");
    }

    public static int getInt(Context context, String key) {
        return context.getSharedPreferences(FILE_NAME, 0).getInt(key, 0);
    }

    public static float getFloat(Context context, String key) {
        return context.getSharedPreferences(FILE_NAME, 0).getFloat(key, 0.0f);
    }

    public static long getLong(Context context, String key) {
        return context.getSharedPreferences(FILE_NAME, 0).getLong(key, 0);
    }

    public static boolean getBoolean(Context context, String key) {
        return context.getSharedPreferences(FILE_NAME, 0).getBoolean(key, false);
    }

    public static void saveString(Context context, String key, String value) {
        context.getSharedPreferences(FILE_NAME, 0).edit().putString(key, value).apply();
    }

    public static void saveInt(Context context, String key, int value) {
        context.getSharedPreferences(FILE_NAME, 0).edit().putInt(key, value).apply();
    }

    public static void saveLong(Context context, String key, long value) {
        context.getSharedPreferences(FILE_NAME, 0).edit().putLong(key, value).apply();
    }

    public static void saveFloat(Context context, String key, float value) {
        context.getSharedPreferences(FILE_NAME, 0).edit().putFloat(key, value).apply();
    }

    public static void saveBoolean(Context context, String key, boolean value) {
        context.getSharedPreferences(FILE_NAME, 0).edit().putBoolean(key, value).apply();
    }

    public static void deleteString(Context context, String key) {
        context.getSharedPreferences(FILE_NAME, 0).edit().remove(key).apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(FILE_NAME, 0).edit().clear().apply();
    }

    public static void putList(Context context, String key, List<? extends Serializable> list) {
        try {
            put(context, key, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <E extends Serializable> List<E> getList(Context context, String key) {
        try {
            return (List) get(context, key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void put(Context context, String key, Object obj) throws IOException {
        if (obj != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            String objectStr = new String(Base64.encode(baos.toByteArray(), 0));
            baos.close();
            oos.close();
            saveString(context, key, objectStr);
        }
    }

    private static Object get(Context context, String key) throws IOException, ClassNotFoundException {
        String wordBase64 = getString(context, key);
        if (TextUtils.isEmpty(wordBase64)) {
            return null;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(wordBase64.getBytes(), 0));
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object obj = ois.readObject();
        bais.close();
        ois.close();
        return obj;
    }
}
