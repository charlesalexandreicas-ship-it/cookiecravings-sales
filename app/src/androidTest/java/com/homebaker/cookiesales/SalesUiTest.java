package com.homebaker.cookiesales;

import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SalesUiTest {
    @Test public void productSaleEditHistoryAndDraftSurviveRecreation() throws Exception {
        SalesDatabase original=SalesDatabase.instance;
        SalesDatabase db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),SalesDatabase.class).build();
        SalesDatabase.instance=db;
        try (ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)) {
            waitFor(a,"Today's sales"); click(a,"Products"); click(a,"Add Product");
            input(a,"Product name","Chocolate Topped"); input(a,"Regular price","20");
            click(a,"Chocolate");
            input(a,"Bundle quantity","3"); input(a,"Bundle price","50"); click(a,"Save Product");
            waitFor(a,"Chocolate Topped"); screenshot("04-products"); click(a,"Record");
            waitFor(a,"Save Sale"); input(a,"Quantity sold","3");
            a.recreate(); waitFor(a,"Save Sale");
            a.onActivity(v->assertEquals("3",((TextView)find(v.getWindow().getDecorView(),"Quantity sold")).getText().toString()));
            AtomicBoolean focused=new AtomicBoolean();long focusUntil=android.os.SystemClock.uptimeMillis()+5000;
            while(!focused.get()&&android.os.SystemClock.uptimeMillis()<focusUntil){a.onActivity(v->focused.set(v.hasWindowFocus()));Thread.sleep(50);}
            assertTrue("App must have window focus (no system overlay)",focused.get());
            a.onActivity(v->{View field=find(v.getWindow().getDecorView(),"Quantity sold");field.requestFocus();androidx.core.view.WindowCompat.getInsetsController(v.getWindow(),field).show(androidx.core.view.WindowInsetsCompat.Type.ime());});
            AtomicBoolean keyboard=new AtomicBoolean();long until=android.os.SystemClock.uptimeMillis()+10000;
            while(!keyboard.get()&&android.os.SystemClock.uptimeMillis()<until){a.onActivity(v->{androidx.core.view.WindowInsetsCompat insets=androidx.core.view.ViewCompat.getRootWindowInsets(v.getWindow().getDecorView());keyboard.set(insets!=null&&insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));});Thread.sleep(50);}
            assertTrue("Keyboard must be visible",keyboard.get());
            a.onActivity(v->{View save=find(v.getWindow().getDecorView(),"Save Sale");android.graphics.Rect visible=new android.graphics.Rect();assertTrue(save.getGlobalVisibleRect(visible));assertEquals("Save action must remain fully visible",save.getHeight(),visible.height());});
            screenshot("06-keyboard");
            a.onActivity(v->v.getSystemService(android.view.inputmethod.InputMethodManager.class).hideSoftInputFromWindow(v.getWindow().getDecorView().getWindowToken(),0));
            screenshot("02-record-sale"); click(a,"Promo"); click(a,"GCash"); click(a,"Save Sale");
            waitFor(a,"Receipt #001"); screenshot("05-receipt");
            SalesStore store=new SalesStore(db); assertEquals(5000,store.snapshot().sales.get(0).total);
            click(a,"Edit Sale"); waitFor(a,"Save Changes"); click(a,"Custom"); input(a,"Amount received","45.50");
            click(a,"Save Changes"); waitFor(a,"Receipt #001");
            assertEquals(4550,store.snapshot().sales.get(0).total); assertEquals(1,store.snapshot().sales.size());
            click(a,"Back"); waitFor(a,"Sales History"); screenshot("03-history");
            input(a,"Search product or receipt","missing"); waitFor(a,"No matching sales");
            input(a,"Search product or receipt",""); waitFor(a,"Chocolate Topped");
            click(a,"Home"); waitFor(a,"Today's sales"); screenshot("01-home");
            a.recreate(); waitFor(a,"Today's sales");
            assertEquals("GCash",store.snapshot().sales.get(0).payment);
        } finally { SalesDatabase.instance=original; db.close(); }
    }
    @Test public void referenceTotalsAndScreensUseIsolatedFixtures() throws Exception {
        SalesDatabase original=SalesDatabase.instance;
        SalesDatabase db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),SalesDatabase.class).build();
        SalesDatabase.instance=db;
        try {
            SalesStore store=new SalesStore(db);
            Product chocolate=fixtureProduct(store,"Chocolate Topped",2000,"chocolate");
            Product plain=fixtureProduct(store,"Plain Cookie",1500,"plain");
            Product smores=fixtureProduct(store,"S’mores Cookie",3000,"smores");
            fixtureSale(store,plain,10,"Cash",null);fixtureSale(store,smores,7,"Cash",null);
            fixtureSale(store,chocolate,3,"Cash","35");fixtureSale(store,chocolate,10,"GCash",null);
            fixtureSale(store,plain,5,"GCash","125");fixtureSale(store,smores,2,"Cash",null);
            fixtureSale(store,plain,5,"GCash",null);fixtureSale(store,chocolate,3,"Cash",null);
            SalesSnapshot data=store.snapshot();assertEquals(91500,SalesSnapshot.revenue(data.sales,null));assertEquals(45,SalesSnapshot.quantity(data.sales));
            assertEquals(51500,SalesSnapshot.revenue(data.sales,"Cash"));assertEquals(40000,SalesSnapshot.revenue(data.sales,"GCash"));
            try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)){
                waitFor(a,"Today's sales");screenshot("reference-home");click(a,"History");waitFor(a,"Sales History");screenshot("reference-history");
                click(a,"Record");waitFor(a,"Save Sale");input(a,"Quantity sold","3");screenshot("reference-record");
            }
        }finally{SalesDatabase.instance=original;db.close();}
    }
    private Product fixtureProduct(SalesStore store,String name,long price,String image){Product p=new Product();p.name=name;p.price=price;p.image=image;p.id=store.saveProduct(p);return p;}
    @Test public void toppingPickerSavesEveryChoiceWithoutPlain() throws Exception {
        SalesDatabase original=SalesDatabase.instance;SalesDatabase db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),SalesDatabase.class).build();SalesDatabase.instance=db;
        try(ActivityScenario<MainActivity> a=ActivityScenario.launch(MainActivity.class)){
            waitFor(a,"Today's sales");click(a,"Products");
            for(int i=0;i<ToppingImages.KEYS.length;i++){
                click(a,"Add Product");waitFor(a,"Topping image");
                a.onActivity(v->assertNull(find(v.getWindow().getDecorView(),"Plain")));
                String label=ToppingImages.LABELS[i],key=ToppingImages.KEYS[i];input(a,"Product name",label);input(a,"Regular price","20");click(a,label);
                if(key.equals("chocolate")){
                    screenshot("topping-picker");
                    a.onActivity(v->{View last=find(v.getWindow().getDecorView(),"Cream-O");last.requestRectangleOnScreen(new android.graphics.Rect(0,0,last.getWidth(),last.getHeight()),true);});
                    screenshot("topping-picker-more");
                }
                click(a,"Save Product");waitFor(a,"Products");
                assertTrue(new SalesStore(db).snapshot().products.stream().anyMatch(p->key.equals(p.image)&&label.equals(p.name)));
            }
            screenshot("topping-products");
        }finally{SalesDatabase.instance=original;db.close();}
    }
    private void fixtureSale(SalesStore store,Product p,int qty,String payment,String custom){SaleDraft d=new SaleDraft();d.productId=p.id;d.quantity=qty;d.payment=payment;if(custom!=null){d.mode="Custom";d.customAmount=custom;}store.saveSale(d);}
    private static View find(View v,String text) {
        View described=description(v,text); return described!=null?described:findText(v,text);
    }
    private static View description(View v,String text) {
        if(text.equals(String.valueOf(v.getContentDescription())))return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=description(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;
    }
    private static View findText(View v,String text) {
        if(text.equals(String.valueOf(v.getContentDescription())) || v instanceof TextView && text.equals(((TextView)v).getText().toString())) return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++){ View found=findText(((ViewGroup)v).getChildAt(i),text); if(found!=null)return found; }
        return null;
    }
    private void waitFor(ActivityScenario<MainActivity> a,String text) throws Exception {
        AtomicBoolean found=new AtomicBoolean(); long until=android.os.SystemClock.uptimeMillis()+10000;
        while(!found.get() && android.os.SystemClock.uptimeMillis()<until){ InstrumentationRegistry.getInstrumentation().waitForIdleSync(); a.onActivity(v->{v.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);View target=find(v.getWindow().getDecorView(),text);found.set(target!=null&&target.isShown());}); if(!found.get())Thread.sleep(50); }
        assertTrue("Missing: "+text,found.get());
    }
    private void click(ActivityScenario<MainActivity> a,String text){ a.onActivity(v->{View target=find(v.getWindow().getDecorView(),text);assertNotNull(text,target);assertTrue(target.performClick());}); }
    private void input(ActivityScenario<MainActivity> a,String text,String value){a.onActivity(v->{TextView target=(TextView)find(v.getWindow().getDecorView(),text);assertNotNull(text,target);target.setText(value);});}
    private void screenshot(String name) throws Exception {
        Thread.sleep(700);
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);
        File dir=new File(ApplicationProvider.getApplicationContext().getExternalFilesDir(null),"sales-screens"); assertTrue(dir.isDirectory()||dir.mkdirs());
        try(ParcelFileDescriptor.AutoCloseInputStream capture=new ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("screencap -p"));
            FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){byte[] buffer=new byte[8192];int count;while((count=capture.read(buffer))!=-1)out.write(buffer,0,count);}
    }
}
