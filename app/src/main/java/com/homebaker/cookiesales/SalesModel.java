package com.homebaker.cookiesales;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.*;

public final class SalesModel extends AndroidViewModel {
    final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final SalesStore store;
    SalesSnapshot data=new SalesSnapshot(new ArrayList<>(),new ArrayList<>());
    String page="Home",query="",filter="Today",start=SaleMath.today(),end=SaleMath.today(),error="";
    long receiptId;
    boolean busy=true,ready=false;
    SaleDraft draft;
    ProductForm productForm;
    public SalesModel(@NonNull Application app){super(app);store=new SalesStore(SalesDatabase.get(app)); load();}
    void notifyUi(){changes.setValue(changes.getValue()+1);}
    private void load(){io.execute(()->{try{SalesSnapshot s=store.snapshot();main.post(()->{data=s;ready=true;busy=false;notifyUi();});}catch(Exception e){fail(e);}});}
    void saveSale(){ if(busy)return;busy=true;error="";notifyUi();SaleDraft saved=draft;
        io.execute(()->{try{long id=store.saveSale(saved);SalesSnapshot s=store.snapshot();main.post(()->{data=s;receiptId=id;draft=null;page="Receipt";busy=false;notifyUi();});}catch(Exception e){fail(e);}});
    }
    void saveProduct(){ if(busy)return;
        final Product product;
        try { product=productForm.toProduct(); } catch(IllegalArgumentException e){error=e.getMessage();notifyUi();return;}
        busy=true;error="";notifyUi();
        io.execute(()->{try{store.saveProduct(product);SalesSnapshot s=store.snapshot();main.post(()->{data=s;productForm=null;page="Products";busy=false;notifyUi();});}catch(Exception e){fail(e);}});
    }
    private void fail(Exception e){main.post(()->{busy=false;error=e instanceof IllegalArgumentException?e.getMessage():"Could not save or load data. Please try again; existing records were kept.";notifyUi();});}
    @Override protected void onCleared(){io.shutdown();}
    static final class ProductForm implements Serializable {
        private static final long serialVersionUID=1L;
        long id;String name="",price="",bundleQuantity="",bundlePrice="",image="chocolate";
        static ProductForm from(Product p){ProductForm f=new ProductForm();f.id=p.id;f.name=p.name;f.price=SaleMath.inputMoney(p.price);f.image=p.image;
            if(p.bundleSize>0){f.bundleQuantity=String.valueOf(p.bundleSize);f.bundlePrice=SaleMath.inputMoney(p.bundlePrice);}return f;}
        Product toProduct(){Product p=new Product();p.id=id;p.name=name;p.price=SaleMath.parseMoney(price);p.image=image;
            if(!bundleQuantity.isEmpty()||!bundlePrice.isEmpty()) {try{p.bundleSize=Integer.parseInt(bundleQuantity);}catch(NumberFormatException e){throw new IllegalArgumentException("Enter a whole bundle quantity.");}p.bundlePrice=SaleMath.parseMoney(bundlePrice);}return p;}
    }
}
