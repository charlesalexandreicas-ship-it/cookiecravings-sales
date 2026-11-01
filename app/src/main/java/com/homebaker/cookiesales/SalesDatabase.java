package com.homebaker.cookiesales;
import android.content.Context;
import androidx.room.*;

@Database(entities={Product.class,Sale.class},version=1,exportSchema=true)
public abstract class SalesDatabase extends RoomDatabase {
    static volatile SalesDatabase instance;
    public abstract SalesDao dao();
    public static synchronized SalesDatabase get(Context context) {
        if(instance==null) instance=Room.databaseBuilder(context.getApplicationContext(),SalesDatabase.class,"cookie_sales.db").build();
        return instance;
    }
}
