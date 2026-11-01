package com.homebaker.cookiesales;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SalesStoreTest {
    SalesDatabase db; SalesStore store;
    @Before public void setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SalesDatabase.class).build(); store = new SalesStore(db); }
    @After public void close() { db.close(); }
    Product product() { Product p = new Product(); p.name="Chocolate Topped"; p.price=2000; p.bundleSize=3; p.bundlePrice=5000; p.image="chocolate"; p.id=store.saveProduct(p); return p; }
    SaleDraft draft(Product p) { SaleDraft d=new SaleDraft(); d.productId=p.id; d.quantity=3; d.day="2026-10-11"; return d; }
    @Test public void saveIsIdempotentAndPricesAreSnapshotted() {
        Product p=product(); SaleDraft d=draft(p); long id=store.saveSale(d);
        assertEquals(id,store.saveSale(d)); assertEquals(1,store.snapshot().sales.size());
        p.name="Updated cookie"; p.price=3000; store.saveProduct(p);
        Sale s=store.snapshot().sales.get(0); assertEquals("Chocolate Topped",s.productName); assertEquals(6000,s.total);
        SaleDraft edit=SaleDraft.from(s); edit.quantity=2; store.saveSale(edit);
        assertEquals(4000,store.snapshot().sales.get(0).total); assertEquals(id,store.snapshot().sales.get(0).id);
    }
    @Test public void editsPaymentCustomAmountAndDateWithoutDuplicate() {
        SaleDraft d=draft(product()); long id=store.saveSale(d); SaleDraft e=SaleDraft.from(store.snapshot().sales.get(0));
        e.mode="Custom"; e.customAmount="55.50"; e.payment="GCash"; e.day="2026-10-10"; e.notes="Reseller";
        assertEquals(id,store.saveSale(e)); Sale s=store.snapshot().sales.get(0);
        assertEquals(5550,s.total); assertEquals("GCash",s.payment); assertEquals("Reseller",s.notes); assertEquals("2026-10-10",s.day);
        e.quantity=0; assertThrows(IllegalArgumentException.class,()->store.saveSale(e)); assertEquals(5550,store.snapshot().sales.get(0).total);
    }
    @Test public void validatesProductAndSaleBoundaries() {
        Product p=product(); SaleDraft d=draft(p); d.mode="Promo"; assertEquals(1,store.saveSale(d)); assertEquals(5000,store.snapshot().sales.get(0).total);
        d=draft(p); d.payment="Other"; SaleDraft invalid=d; assertThrows(IllegalArgumentException.class,()->store.saveSale(invalid));
        p.name=" "; assertThrows(IllegalArgumentException.class,()->store.saveProduct(p));
        p.name="Cookie"; p.bundleSize=1; assertThrows(IllegalArgumentException.class,()->store.saveProduct(p));
        assertEquals(1,store.snapshot().products.size()); assertEquals(1,store.snapshot().sales.size());
    }
    @Test public void filtersByDatesAndLiteralSearch() {
        Product p=product(); SaleDraft d=draft(p); store.saveSale(d);
        SaleDraft other=draft(p); other.day="2026-10-10"; store.saveSale(other);
        SalesSnapshot s=store.snapshot(); assertEquals(1,s.filter("2026-10-11","2026-10-11","chocolate").size());
        assertEquals(2,s.filter("2026-10-05","2026-10-11","").size());
        assertEquals(0,s.filter("2026-10-05","2026-10-11","%'").size());
        assertEquals(1,s.filter("2026-10-05","2026-10-11","#001").size());
    }
    @Test public void changingProductUsesNewSnapshot() {
        Product p=product(); SaleDraft d=draft(p); store.saveSale(d);
        Product second=new Product(); second.name="Plain"; second.price=1500; second.id=store.saveProduct(second);
        SaleDraft e=SaleDraft.from(store.snapshot().sales.get(0)); e.productId=second.id; store.saveSale(e);
        assertEquals(4500,store.snapshot().sales.get(0).total); assertEquals("Plain",store.snapshot().sales.get(0).productName);
    }
    @Test public void diskDatabaseSurvivesCloseAndReopen() {
        android.content.Context context=ApplicationProvider.getApplicationContext();
        String name="sales-test-"+java.util.UUID.randomUUID()+".db";
        SalesDatabase file=Room.databaseBuilder(context,SalesDatabase.class,name).build();
        try {
            SalesStore disk=new SalesStore(file);Product p=new Product();p.name="Persisted cookie";p.price=1500;p.id=disk.saveProduct(p);
            SaleDraft d=draft(p);long id=disk.saveSale(d);file.close();
            file=Room.databaseBuilder(context,SalesDatabase.class,name).build();
            Sale saved=new SalesStore(file).snapshot().sales.get(0);assertEquals(id,saved.id);assertEquals(4500,saved.total);
            assertEquals("Persisted cookie",saved.productName);assertEquals(1,file.getOpenHelper().getReadableDatabase().getVersion());
        } finally {file.close();context.deleteDatabase(name);}
    }
    @Test public void malformedDatesAndUnknownProductsDoNotWrite() {
        SaleDraft d=draft(product());d.day="2026-02-30";assertThrows(IllegalArgumentException.class,()->store.saveSale(d));
        d.day="2100-01-01";assertThrows(IllegalArgumentException.class,()->store.saveSale(d));
        d.day="2026-10-10";d.productId=999;assertThrows(IllegalArgumentException.class,()->store.saveSale(d));
        assertTrue(store.snapshot().sales.isEmpty());
    }
    @Test public void allToppingsPersistAndLegacyRecordsRemainIntact() {
        Product legacy=product();legacy.image="plain";store.saveProduct(legacy);
        long receipt=store.saveSale(draft(legacy));
        for(String key:ToppingImages.KEYS){Product p=new Product();p.name=ToppingImages.label(key);p.price=2500;p.image=key;p.id=store.saveProduct(p);assertEquals(key,store.snapshot().product(p.id).image);store.saveSale(draft(p));}
        legacy.image="cadbury";store.saveProduct(legacy);
        assertEquals("plain",store.snapshot().sale(receipt).image);assertEquals(6000,store.snapshot().sale(receipt).total);
        assertEquals("cadbury",store.snapshot().product(legacy.id).image);
        legacy.image="invalid";assertThrows(IllegalArgumentException.class,()->store.saveProduct(legacy));
        assertEquals("cadbury",store.snapshot().product(legacy.id).image);
    }
}
