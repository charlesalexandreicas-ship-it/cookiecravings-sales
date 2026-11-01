package com.homebaker.cookiesales;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName="products")
public class Product implements Serializable {
    private static final long serialVersionUID=1L;
    @PrimaryKey(autoGenerate=true) public long id;
    @NonNull public String name="";
    public long price;
    public int bundleSize;
    public long bundlePrice;
    @NonNull public String image="chocolate";
}
