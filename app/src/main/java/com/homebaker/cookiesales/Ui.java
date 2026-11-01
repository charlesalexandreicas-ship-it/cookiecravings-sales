package com.homebaker.cookiesales;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import java.util.function.Consumer;

final class Ui {
    static final int CREAM=Color.rgb(255,249,246),PINK=Color.rgb(251,234,236),WINE=Color.rgb(166,9,54),INK=Color.rgb(68,34,25),MUTED=Color.rgb(120,103,98),LINE=Color.rgb(230,213,208),GREEN=Color.rgb(32,114,62);
    final Context c;
    Ui(Context c){this.c=c;}
    int dp(float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    boolean compact(){return c.getResources().getConfiguration().screenWidthDp<360||c.getResources().getConfiguration().fontScale>1.15f;}
    GradientDrawable bg(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border)d.setStroke(dp(1),LINE);return d;}
    LinearLayout column(){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    void add(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    void gap(LinearLayout p,int n){View v=new View(c);p.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    TextView label(LinearLayout p,String s){TextView t=text(s,14,MUTED,true);add(p,t);gap(p,6);return t;}
    LinearLayout card(LinearLayout p,int color){LinearLayout b=column();b.setPadding(dp(16),dp(16),dp(16),dp(16));b.setBackground(bg(color,16,color==CREAM));add(p,b);return b;}
    void line(LinearLayout p){gap(p,12);View line=new View(c);line.setBackgroundColor(LINE);p.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));gap(p,12);}
    MaterialButton button(String s,boolean primary,Runnable action){MaterialButton b=new MaterialButton(c);b.setText(s);b.setTextSize(16);b.setAllCaps(false);b.setLetterSpacing(0);b.setStateListAnimator(null);b.setElevation(0);b.setIconTint(ColorStateList.valueOf(primary?Color.WHITE:WINE));b.setTypeface(null,Typeface.BOLD);b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setInsetTop(0);b.setInsetBottom(0);b.setCornerRadius(dp(14));b.setTextColor(primary?Color.WHITE:WINE);b.setBackgroundTintList(ColorStateList.valueOf(primary?WINE:CREAM));if(!primary){b.setStrokeWidth(dp(1));b.setStrokeColor(ColorStateList.valueOf(LINE));}b.setOnClickListener(v->action.run());return b;}
    TextView action(String s,Runnable action){TextView t=text(s,14,WINE,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));t.setPadding(dp(4),0,dp(4),0);t.setOnClickListener(v->action.run());t.setFocusable(true);t.setBackground(bg(CREAM,8,false));return t;}
    ImageView icon(int res,int color){ImageView v=new ImageView(c);v.setImageResource(res);v.setImageTintList(ColorStateList.valueOf(color));v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
    ImageView photo(String key,int size){ImageView v=new ImageView(c);v.setImageResource(ToppingImages.drawable(key));v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setBackground(bg(PINK,12,false));v.setPadding(dp(3),dp(3),dp(3),dp(3));v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
    EditText input(LinearLayout p,String label,String value,boolean numeric,Consumer<String> change){label(p,label);EditText e=new EditText(c);e.setTextColor(INK);e.setTextSize(17);e.setSingleLine(true);e.setPadding(dp(14),dp(10),dp(14),dp(10));e.setMinHeight(dp(52));e.setBackground(bg(CREAM,10,true));e.setContentDescription(label);e.setInputType(numeric?android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(numeric?12:500)});e.setText(value);e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){change.accept(s.toString());}public void afterTextChanged(Editable s){}});add(p,e);return e;}
    void pair(LinearLayout p,String left,String right,int color,boolean large){LinearLayout r=compact()?column():row();add(r,text(left,large?17:14,INK,large));if(!compact()){r.removeAllViews();weight(r,text(left,large?17:14,INK,large));}TextView amount=text(right,large?24:16,color,true);r.addView(amount);add(p,r);}
}
