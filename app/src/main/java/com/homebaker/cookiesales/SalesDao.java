package com.homebaker.cookiesales;
import androidx.room.*;
import java.util.List;

@Dao
public interface SalesDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE,id") List<Product> products();
    @Query("SELECT * FROM products WHERE id=:id") Product product(long id);
    @Insert long insertProduct(Product product);
    @Update int updateProduct(Product product);
    @Query("SELECT * FROM sales ORDER BY day DESC,createdAt DESC,id DESC") List<Sale> sales();
    @Query("SELECT * FROM sales WHERE id=:id") Sale sale(long id);
    @Query("SELECT * FROM sales WHERE requestId=:token") Sale byToken(String token);
    @Insert long insertSale(Sale sale);
    @Update int updateSale(Sale sale);
}
