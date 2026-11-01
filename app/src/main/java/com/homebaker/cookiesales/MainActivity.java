package com.homebaker.cookiesales;

import static com.homebaker.cookiesales.Ui.*;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public final class MainActivity extends AppCompatActivity {
    private SalesModel model;
    private Ui ui;
    private LinearLayout screen,content,historyList,historySummary,saleSummary;
    private BottomNavigationView nav;
    private EditText amount;
    private boolean syncingNav;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);ui=new Ui(this);setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_root),(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsetsCompat.CONSUMED;
        });
        model=new ViewModelProvider(this).get(SalesModel.class);
        if(state!=null&&!model.ready) restore(state);
        screen=findViewById(R.id.screen);nav=findViewById(R.id.navigation);setupNav();
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){back();}});
        model.changes.observe(this,n->render());
    }
    @SuppressWarnings("deprecation") private void restore(Bundle b) {
        model.page=b.getString("page","Home");model.draft=(SaleDraft)b.getSerializable("draft");
        model.productForm=(SalesModel.ProductForm)b.getSerializable("productForm");model.receiptId=b.getLong("receiptId");
        model.query=b.getString("query","");model.filter=b.getString("filter","Today");
        model.start=b.getString("start",SaleMath.today());model.end=b.getString("end",SaleMath.today());
    }
    @Override protected void onSaveInstanceState(Bundle b) {
        b.putString("page",model.page);b.putSerializable("draft",model.draft);b.putSerializable("productForm",model.productForm);
        b.putLong("receiptId",model.receiptId);b.putString("query",model.query);b.putString("filter",model.filter);
        b.putString("start",model.start);b.putString("end",model.end);super.onSaveInstanceState(b);
    }
    private void setupNav() {
        String[] labels={"Home","Record","History","Products"};int[] icons={R.drawable.ic_ref_home,R.drawable.ic_plus,R.drawable.ic_history,R.drawable.ic_ref_cookie};
        for(int i=0;i<labels.length;i++)MenuItemCompat.setContentDescription(nav.getMenu().add(0,i+1,i,labels[i]).setIcon(icons[i]),labels[i]);
        ColorStateList colors=new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{WINE,MUTED});
        nav.setItemIconTintList(colors);nav.setItemTextColor(colors);nav.setItemActiveIndicatorEnabled(false);
        nav.setLabelVisibilityMode(com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
        nav.setOnItemSelectedListener(item->{if(syncingNav)return true;if(model.busy)return false;navigate(labels[item.getItemId()-1]);return true;});
    }
    private void navigate(String page){if(model.busy)return;hideKeyboard();model.error="";
        if(page.equals("Record")&&model.draft==null){model.draft=new SaleDraft();if(!model.data.products.isEmpty())model.draft.productId=model.data.products.get(0).id;}
        model.page=page;render();
    }
    private void hideKeyboard(){getSystemService(InputMethodManager.class).hideSoftInputFromWindow(screen.getWindowToken(),0);}
    private void back(){if(model.busy)return;
        if(model.page.equals("Record")||model.page.equals("ProductForm")){
            new MaterialAlertDialogBuilder(this).setTitle("Discard unsaved changes?").setMessage("Saved records will not be changed.")
                    .setNegativeButton("Keep editing",null).setPositiveButton("Discard",(d,w)->{model.draft=null;model.productForm=null;navigate("Home");}).show();
        }else if(model.page.equals("Receipt")){Sale s=model.data.sale(model.receiptId);if(s!=null){model.filter="Custom";model.start=s.day;model.end=s.day;model.query="";}navigate("History");}
        else if(model.page.equals("Home"))finish();else navigate("Home");
    }
    private void render(){
        screen.removeAllViews();amount=null;saleSummary=null;
        boolean main=Arrays.asList("Home","History","Products").contains(model.page);nav.setVisibility(main?View.VISIBLE:View.GONE);
        syncingNav=true;nav.setSelectedItemId(model.page.equals("Products")?4:model.page.equals("History")?3:1);syncingNav=false;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=ui.column();content.setPadding(ui.dp(18),ui.dp(16),ui.dp(18),ui.dp(20));content.setFocusableInTouchMode(true);
        scroll.addView(content);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(!model.ready){ui.add(content,ui.text(model.error.isEmpty()?"Loading your sales…":model.error,17,INK,false));return;}
        if(!model.error.isEmpty()){TextView err=ui.text(model.error,15,WINE,true);err.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);ui.add(content,err);ui.gap(content,12);}
        switch(model.page){case "Home":home();break;case "Record":record();break;case "History":history();break;case "Products":products();break;case "ProductForm":productForm();break;case "Receipt":receipt();break;default:model.page="Home";home();}
        if(model.busy){enable(screen,false);enable(nav,false);}else enable(nav,true);
    }
    private void enable(View v,boolean value){v.setEnabled(value);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)enable(((ViewGroup)v).getChildAt(i),value);}
    private void heading(String title,boolean back){
        LinearLayout row=ui.row();
        if(back){ImageButton b=new ImageButton(this);b.setImageResource(R.drawable.ic_arrow_back);b.setImageTintList(ColorStateList.valueOf(WINE));b.setBackgroundColor(CREAM);b.setContentDescription("Back");b.setOnClickListener(v->back());row.addView(b,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));}
        TextView titleView=ui.text(title,back?24:30,INK,true);ViewCompat.setAccessibilityHeading(titleView,true);ui.weight(row,titleView);ui.add(content,row);ui.gap(content,16);
    }
    private void footer(String label,Runnable action){LinearLayout bottom=ui.column();bottom.setPadding(ui.dp(18),ui.dp(8),ui.dp(18),ui.dp(8));ui.add(bottom,ui.button(model.busy?"Saving…":label,true,action));ui.add(screen,bottom);}
    private void home(){
        LinearLayout brand=ui.row(),name=ui.column();ui.add(name,ui.text("CookieCravings",25,WINE,true));ui.add(name,ui.text("Sales",14,MUTED,false));ui.weight(brand,name);
        ImageButton receipt=new ImageButton(this);receipt.setImageResource(R.drawable.ic_ref_document);receipt.setImageTintList(ColorStateList.valueOf(WINE));receipt.setBackgroundColor(CREAM);receipt.setContentDescription("Open sales history");receipt.setOnClickListener(v->navigate("History"));brand.addView(receipt,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));ui.add(content,brand);ui.gap(content,22);
        ui.add(content,ui.text("Today's sales",30,INK,true));ui.add(content,ui.text(SaleMath.displayDate(SaleMath.today()),15,MUTED,false));ui.gap(content,18);
        List<Sale> today=model.data.filter(SaleMath.today(),SaleMath.today(),"");LinearLayout summary=ui.card(content,PINK);
        ui.add(summary,ui.text("Sales recorded",16,INK,false));ui.gap(summary,4);ui.add(summary,ui.text(SaleMath.money(SalesSnapshot.revenue(today,null)),36,WINE,true));ui.gap(summary,6);
        ui.add(summary,ui.text(SalesSnapshot.quantity(today)+" cookies sold · "+today.size()+" transactions",14,MUTED,false));ui.gap(content,10);
        LinearLayout payments=ui.compact()?ui.column():ui.row();
        for(String payment:new String[]{"Cash","GCash"}){LinearLayout tile=ui.column();tile.setPadding(ui.dp(14),ui.dp(12),ui.dp(14),ui.dp(12));tile.setBackground(ui.bg(CREAM,14,true));ui.add(tile,ui.text(payment,14,INK,false));ui.add(tile,ui.text(SaleMath.money(SalesSnapshot.revenue(today,payment)),23,INK,true));if(ui.compact()){ui.add(payments,tile);ui.gap(payments,8);}else{if(payments.getChildCount()>0)payments.addView(new View(this),new LinearLayout.LayoutParams(ui.dp(10),1));ui.weight(payments,tile);}}
        ui.add(content,payments);ui.gap(content,12);ui.add(content,ui.button("Record Sale",true,()->navigate("Record")));ui.gap(content,20);
        LinearLayout recent=ui.row();ui.weight(recent,ui.text("Recent sales",20,INK,true));recent.addView(ui.action("View all",()->navigate("History")));ui.add(content,recent);
        if(model.data.sales.isEmpty())empty(content,"No sales yet","Add your products, then record your first sale.");
        else for(int i=0;i<Math.min(3,model.data.sales.size());i++){saleRow(content,model.data.sales.get(i),false);ui.gap(content,10);}
    }
    private String time(Sale s){return new SimpleDateFormat("h:mm a",Locale.US).format(new Date(s.createdAt));}
    private void saleRow(LinearLayout parent,Sale s,boolean detailed){
        LinearLayout card=ui.card(parent,detailed?PINK:CREAM);
        if(detailed){ui.pair(card,s.receipt(),s.day.equals(SaleMath.today())?time(s):SaleMath.displayDate(s.day),MUTED,false);ui.gap(card,6);}
        LinearLayout row=ui.row();if(!detailed){row.addView(ui.photo(s.image,54));row.addView(new View(this),new LinearLayout.LayoutParams(ui.dp(10),1));}
        LinearLayout name=ui.column();ui.add(name,ui.text(s.productName,17,INK,true));ui.gap(name,3);ui.add(name,ui.text(s.quantity+" cookies · "+s.payment+(detailed?"":" · "+(s.day.equals(SaleMath.today())?time(s):SaleMath.displayDate(s.day))),12,MUTED,false));
        ui.weight(row,name);if(!ui.compact())row.addView(ui.text(SaleMath.money(s.total),18,WINE,true));ui.add(card,row);
        if(ui.compact()){ui.gap(card,6);ui.add(card,ui.text(SaleMath.money(s.total),20,WINE,true));}
        card.setFocusable(true);card.setContentDescription(s.receipt()+", "+s.productName+", "+s.quantity+" cookies, "+SaleMath.money(s.total)+", "+s.payment+". View receipt");
        card.setOnClickListener(v->openReceipt(s.id));
        if(detailed){ui.line(card);LinearLayout actions=ui.row();ui.weight(actions,ui.action("View receipt",()->openReceipt(s.id)));ui.weight(actions,ui.action("Edit sale",()->edit(s)));ui.add(card,actions);}
    }
    private void openReceipt(long id){model.receiptId=id;navigate("Receipt");}
    private void edit(Sale s){model.draft=SaleDraft.from(s);navigate("Record");}
    private void empty(LinearLayout parent,String title,String message){LinearLayout c=ui.card(parent,PINK);ui.add(c,ui.text(title,19,INK,true));ui.gap(c,8);ui.add(c,ui.text(message,15,MUTED,false));}
    private void products(){heading("Products",false);ui.add(content,ui.text("Set your cookies, prices and bundle offers.",15,MUTED,false));ui.gap(content,14);ui.add(content,ui.button("Add Product",true,()->{model.productForm=new SalesModel.ProductForm();navigate("ProductForm");}));ui.gap(content,18);
        if(model.data.products.isEmpty())empty(content,"Your product list is empty","Add a cookie and its selling price to start recording sales.");
        for(Product p:model.data.products){LinearLayout card=ui.card(content,CREAM),r=ui.row();r.addView(ui.photo(p.image,62));LinearLayout label=ui.column();label.setPadding(ui.dp(14),0,0,0);ui.add(label,ui.text(p.name,19,INK,true));ui.add(label,ui.text(SaleMath.money(p.price)+" each",17,WINE,true));if(p.bundleSize>0)ui.add(label,ui.text(p.bundleSize+" for "+SaleMath.money(p.bundlePrice),13,MUTED,false));ui.weight(r,label);ui.add(card,r);ui.add(card,ui.action("Edit product",()->{model.productForm=SalesModel.ProductForm.from(p);navigate("ProductForm");}));ui.gap(content,12);}
    }
    private void productForm(){if(model.productForm==null)model.productForm=new SalesModel.ProductForm();SalesModel.ProductForm f=model.productForm;
        heading(f.id==0?"Add Product":"Edit Product",true);
        ui.input(content,"Product name",f.name,false,s->f.name=s);ui.gap(content,16);
        ui.input(content,"Regular price",f.price,true,s->f.price=s);ui.gap(content,18);
        ui.label(content,"Topping image");ui.add(content,ui.text("Toppings only — choose what goes on your cookie.",14,MUTED,false));ui.gap(content,10);
        if(ToppingImages.drawable(f.image)==0){ui.add(content,ui.text("Choose a topping to replace the old plain image.",14,MUTED,false));ui.gap(content,8);}
        for(int i=0;i<ToppingImages.KEYS.length;i+=2){LinearLayout choices=ui.row();
            for(int j=i;j<Math.min(i+2,ToppingImages.KEYS.length);j++){String key=ToppingImages.KEYS[j],label=ToppingImages.LABELS[j];boolean selected=key.equals(f.image);
                LinearLayout tile=ui.column();tile.setGravity(Gravity.CENTER);tile.setPadding(ui.dp(8),ui.dp(10),ui.dp(8),ui.dp(10));
                android.graphics.drawable.GradientDrawable background=ui.bg(selected?PINK:CREAM,12,true);if(selected)background.setStroke(ui.dp(2),WINE);tile.setBackground(background);
                tile.addView(ui.photo(key,60));TextView caption=ui.text(label,14,selected?WINE:INK,true);caption.setGravity(Gravity.CENTER);caption.setMinHeight(ui.dp(40));ui.add(tile,caption);
                tile.setContentDescription(label);tile.setFocusable(true);tile.setSelected(selected);tile.setOnClickListener(v->{f.image=key;render();});
                if(choices.getChildCount()>0)choices.addView(new View(this),new LinearLayout.LayoutParams(ui.dp(10),1));ui.weight(choices,tile);
            }ui.add(content,choices);ui.gap(content,10);
        }ui.gap(content,8);
        LinearLayout promo=ui.card(content,PINK);ui.add(promo,ui.text("Bundle offer (optional)",19,INK,true));ui.gap(promo,6);ui.add(promo,ui.text("For example, 3 for ₱50. Leave both blank for no promo.",14,MUTED,false));ui.gap(promo,12);
        ui.input(promo,"Bundle quantity",f.bundleQuantity,true,s->f.bundleQuantity=s).setInputType(android.text.InputType.TYPE_CLASS_NUMBER);ui.gap(promo,12);ui.input(promo,"Bundle price",f.bundlePrice,true,s->f.bundlePrice=s);
        ui.gap(content,16);ui.add(content,ui.text("Price updates apply to new sales. Existing receipts keep their recorded prices.",14,MUTED,false));footer("Save Product",()->{hideKeyboard();model.saveProduct();});
    }
    private void segments(LinearLayout parent,String[] values,String selected,Consumer<String> onSelect){LinearLayout row=ui.row();for(String value:values){MaterialButton b=ui.button(value,value.equals(selected),()->onSelect.accept(value));b.setTextSize(14);b.setPadding(ui.dp(4),0,ui.dp(4),0);b.setContentDescription(value);if(row.getChildCount()>0)row.addView(new View(this),new LinearLayout.LayoutParams(ui.dp(6),1));ui.weight(row,b);}ui.add(parent,row);}
    private Product priceProduct(){SaleDraft d=model.draft;Product p=model.data.product(d.productId);Sale old=model.data.sale(d.id);
        if(old!=null&&old.productId==d.productId){Product historic=new Product();historic.id=old.productId;historic.name=old.productName;historic.price=old.unitPrice;historic.bundleSize=old.bundleSize;historic.bundlePrice=old.bundlePrice;historic.image=old.image;return historic;}return p;
    }
    private void record(){if(model.draft==null)model.draft=new SaleDraft();SaleDraft d=model.draft;
        heading(d.id==0?"Record Sale":"Edit Sale",true);
        if(model.data.products.isEmpty()){empty(content,"Add a product first","Your sales app has its own product list. Add a cookie and price before recording a sale.");ui.gap(content,16);ui.add(content,ui.button("Add Product",true,()->{model.productForm=new SalesModel.ProductForm();navigate("ProductForm");}));return;}
        if(d.productId==0)d.productId=model.data.products.get(0).id;Product p=priceProduct();
        MaterialButton date=ui.button(SaleMath.displayDate(d.day),false,()->datePicker(d.day,s->{d.day=s;render();}));date.setIconResource(R.drawable.ic_ref_calendar);date.setContentDescription("Sale date");ui.add(content,date);ui.gap(content,14);
        ui.label(content,"Product");LinearLayout product=ui.card(content,CREAM),r=ui.row();r.addView(ui.photo(p.image,58));LinearLayout info=ui.column();info.setPadding(ui.dp(12),0,ui.dp(6),0);ui.add(info,ui.text(p.name,18,INK,true));ui.add(info,ui.text(SaleMath.money(p.price)+" each",14,MUTED,false));ui.weight(r,info);r.addView(ui.icon(R.drawable.ic_ref_chevron,MUTED),new LinearLayout.LayoutParams(ui.dp(20),ui.dp(24)));ui.add(product,r);product.setContentDescription("Choose product");product.setFocusable(true);product.setOnClickListener(v->chooseProduct());ui.gap(content,16);
        ui.label(content,"Quantity sold");LinearLayout count=ui.row();MaterialButton minus=ui.button("−",false,()->{if(d.quantity>1){d.quantity--;render();}});minus.setContentDescription("Decrease quantity");count.addView(minus,new LinearLayout.LayoutParams(ui.dp(52),ui.dp(52)));
        LinearLayout box=ui.column();EditText qty=ui.input(box,"Quantity sold",String.valueOf(d.quantity),true,s->{try{d.quantity=Integer.parseInt(s);}catch(NumberFormatException ex){d.quantity=0;}refreshTotal();});box.removeViewAt(0);box.removeViewAt(0);qty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);qty.setGravity(Gravity.CENTER);ui.weight(count,box);
        MaterialButton plus=ui.button("+",false,()->{if(d.quantity<999999){d.quantity++;render();}});plus.setContentDescription("Increase quantity");count.addView(plus,new LinearLayout.LayoutParams(ui.dp(52),ui.dp(52)));ui.add(content,count);ui.gap(content,16);
        ui.label(content,"Pricing");segments(content,new String[]{"Regular","Promo","Custom"},d.mode,s->{d.mode=s;render();});
        if(d.mode.equals("Promo")){ui.gap(content,6);ui.add(content,ui.text(p.bundleSize>0?p.bundleSize+" for "+SaleMath.money(p.bundlePrice)+" · extras at regular price":"No bundle offer. Add one in Products, or choose Regular / Custom.",13,p.bundleSize>0?MUTED:WINE,false));}
        ui.gap(content,16);ui.label(content,"Payment method");segments(content,new String[]{"Cash","GCash"},d.payment,s->{d.payment=s;render();});ui.gap(content,16);
        amount=ui.input(content,"Amount received",d.customAmount,true,s->{if(d.mode.equals("Custom")){d.customAmount=s;refreshTotal();}});amount.setEnabled(d.mode.equals("Custom"));
        if(d.mode.equals("Custom")){ui.gap(content,4);ui.add(content,ui.text("Net sale amount, not cash tendered. No change or balances are tracked.",12,MUTED,false));}
        ui.gap(content,12);LinearLayout notes=ui.column();ui.input(notes,"Customer / notes",d.notes,false,s->d.notes=s).setSingleLine(false);notes.setVisibility(d.notes.isEmpty()?View.GONE:View.VISIBLE);
        ui.add(content,ui.action("Customer / notes (optional)",()->notes.setVisibility(notes.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE)));ui.add(content,notes);ui.gap(content,12);
        saleSummary=ui.card(content,PINK);refreshTotal();
        footer(d.id==0?"Save Sale":"Save Changes",()->{hideKeyboard();model.saveSale();});
    }
    private void chooseProduct(){String[] names=new String[model.data.products.size()];for(int i=0;i<names.length;i++){Product p=model.data.products.get(i);names[i]=p.name+" · "+SaleMath.money(p.price);}
        new MaterialAlertDialogBuilder(this).setTitle("Choose product").setItems(names,(dialog,which)->{model.draft.productId=model.data.products.get(which).id;model.draft.mode="Regular";render();}).setNegativeButton("Cancel",null).show();}
    private void refreshTotal(){if(saleSummary==null)return;saleSummary.removeAllViews();SaleDraft d=model.draft;Product p=priceProduct();
        try{long total=SaleMath.total(p.price,d.quantity,d.mode,p.bundleSize,p.bundlePrice,d.mode.equals("Custom")?SaleMath.parseMoney(d.customAmount):0);
            if(!d.mode.equals("Custom")&&amount!=null&&!amount.getText().toString().equals(SaleMath.inputMoney(total)))amount.setText(SaleMath.inputMoney(total));
            ui.pair(saleSummary,d.quantity+" × "+SaleMath.money(p.price),SaleMath.money(p.price*d.quantity),INK,false);ui.gap(saleSummary,8);
            long discount=p.price*d.quantity-total;ui.pair(saleSummary,discount>=0?"Discount":"Extra received",SaleMath.money(Math.abs(discount)),INK,false);ui.line(saleSummary);ui.pair(saleSummary,"Sale total",SaleMath.money(total),WINE,true);
        }catch(IllegalArgumentException e){ui.add(saleSummary,ui.text(e.getMessage(),14,WINE,false));}
    }
    private void history(){heading("Sales History",false);
        LinearLayout searchBox=ui.column();EditText search=ui.input(searchBox,"Search product or receipt",model.query,false,s->{model.query=s;refreshHistory();});searchBox.removeViewAt(0);searchBox.removeViewAt(0);ui.add(content,searchBox);search.setHint("Search product or receipt");search.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_ref_search,0,0,0);search.setCompoundDrawablePadding(ui.dp(8));ui.gap(content,14);
        segments(content,new String[]{"Today","This week","Custom"},model.filter,s->{if(s.equals("Custom")){datePicker(model.start,start->datePicker(start,end->{if(end.compareTo(start)<0){model.error="End date must be on or after start date.";render();return;}model.filter="Custom";model.start=start;model.end=end;render();}));}else{model.filter=s;model.end=SaleMath.today();model.start=s.equals("Today")?model.end:SaleMath.weekStart(model.end);render();}});ui.gap(content,16);
        historySummary=ui.card(content,PINK);ui.gap(content,18);ui.add(content,ui.text("Latest transactions",20,INK,true));ui.gap(content,12);historyList=ui.column();ui.add(content,historyList);refreshHistory();
        ui.gap(content,12);ui.add(content,ui.button("Record another sale",false,()->navigate("Record")));
    }
    private void refreshHistory(){if(historyList==null||!model.page.equals("History"))return;
        List<Sale> filtered=model.data.filter(model.start,model.end,model.query);historySummary.removeAllViews();historyList.removeAllViews();
        ui.add(historySummary,ui.text(SaleMath.displayDate(model.start)+(model.start.equals(model.end)?"":" – "+SaleMath.displayDate(model.end)),13,MUTED,false));ui.gap(historySummary,6);
        ui.add(historySummary,ui.text(SaleMath.money(SalesSnapshot.revenue(filtered,null)),30,WINE,true));ui.add(historySummary,ui.text(filtered.size()+" transactions · "+SalesSnapshot.quantity(filtered)+" cookies sold",14,MUTED,false));
        if(!model.query.isEmpty())ui.add(historySummary,ui.text("Totals for matching results",12,MUTED,false));
        if(filtered.isEmpty())empty(historyList,"No matching sales","Try another date range or search, or record your first sale.");
        for(Sale s:filtered){saleRow(historyList,s,true);ui.gap(historyList,12);}
    }
    private void receipt(){Sale s=model.data.sale(model.receiptId);if(s==null){heading("Receipt unavailable",true);return;}
        heading("Receipt "+s.receipt(),true);LinearLayout card=ui.card(content,CREAM);
        ui.add(card,ui.text("CookieCravings Sales",22,WINE,true));ui.add(card,ui.text(SaleMath.displayDate(s.day)+" · "+s.payment,15,MUTED,false));ui.line(card);
        ui.add(card,ui.text(s.productName,23,INK,true));ui.gap(card,8);ui.pair(card,"Quantity",String.valueOf(s.quantity),INK,false);ui.gap(card,8);ui.pair(card,"Regular price",SaleMath.money(s.unitPrice),INK,false);ui.gap(card,8);ui.pair(card,"Pricing",s.mode,INK,false);
        if(s.mode.equals("Promo")){ui.gap(card,8);ui.pair(card,"Bundle",s.bundleSize+" for "+SaleMath.money(s.bundlePrice),INK,false);}
        ui.line(card);ui.pair(card,"Sales recorded",SaleMath.money(s.total),WINE,true);
        if(!s.notes.isEmpty()){ui.line(card);ui.add(card,ui.text("Customer / notes",14,MUTED,true));ui.add(card,ui.text(s.notes,16,INK,false));}
        ui.gap(content,14);ui.add(content,ui.text("Saved on this device. This is a sales record, not an official tax invoice.",13,MUTED,false));ui.gap(content,16);ui.add(content,ui.button("Edit Sale",false,()->edit(s)));ui.gap(content,10);
        ui.add(content,ui.button("Record another sale",true,()->navigate("Record")));
    }
    private void datePicker(String initial,Consumer<String> chosen){Calendar c=SaleMath.calendar(initial);DatePickerDialog dialog=new DatePickerDialog(this,(v,y,m,d)->chosen.accept(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));dialog.getDatePicker().setMinDate(SaleMath.calendar("1970-01-01").getTimeInMillis());dialog.getDatePicker().setMaxDate(System.currentTimeMillis());dialog.show();}
}
