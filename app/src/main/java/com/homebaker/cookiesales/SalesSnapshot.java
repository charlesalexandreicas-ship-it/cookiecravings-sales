package com.homebaker.cookiesales;
import java.util.*;

public final class SalesSnapshot {
    public final List<Product> products;
    public final List<Sale> sales;
    public SalesSnapshot(List<Product> products,List<Sale> sales) { this.products=products;this.sales=sales; }
    public Product product(long id) { for(Product p:products) if(p.id==id)return p;return null; }
    public Sale sale(long id) { for(Sale s:sales)if(s.id==id)return s;return null; }
    public List<Sale> filter(String start,String end,String query) {
        String q=query.trim().toLowerCase(Locale.ROOT); List<Sale> found=new ArrayList<>();
        for(Sale s:sales) if(s.day.compareTo(start)>=0 && s.day.compareTo(end)<=0 &&
                (s.productName.toLowerCase(Locale.ROOT).contains(q)||s.receipt().contains(q))) found.add(s);
        return found;
    }
    public static long revenue(List<Sale> sales,String payment) {
        long sum=0;for(Sale s:sales)if(payment==null||payment.equals(s.payment))sum=Math.addExact(sum,s.total);return sum;
    }
    public static long quantity(List<Sale> sales) { long n=0;for(Sale s:sales)n=Math.addExact(n,s.quantity);return n; }
}
