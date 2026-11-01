package com.homebaker.cookiesales;
import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName="sales",indices={@Index(value="requestId",unique=true),@Index("productId"),@Index("day")},
        foreignKeys=@ForeignKey(entity=Product.class,parentColumns="id",childColumns="productId",onDelete=ForeignKey.RESTRICT))
public class Sale {
    @PrimaryKey(autoGenerate=true) public long id;
    @NonNull public String requestId="";
    public long productId;
    @NonNull public String productName="";
    @NonNull public String image="plain";
    public long unitPrice;
    public int quantity;
    @NonNull public String mode="Regular";
    public int bundleSize;
    public long bundlePrice;
    public long total;
    @NonNull public String payment="Cash";
    @NonNull public String day="";
    @NonNull public String notes="";
    public long createdAt;
    public long updatedAt;
    public String receipt() { return String.format(java.util.Locale.US,"#%03d",id); }
}
