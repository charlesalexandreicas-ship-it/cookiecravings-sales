package com.homebaker.cookiesales;

/** Called on the model's single IO executor, never on the UI thread. */
public final class SalesStore {
    private final SalesDatabase db;
    public SalesStore(SalesDatabase db) { this.db=db; }
    public SalesSnapshot snapshot() { return db.runInTransaction(()->new SalesSnapshot(db.dao().products(),db.dao().sales())); }
    public long saveProduct(Product p) {
        return db.runInTransaction(()->{
            if(p.name==null||p.name.trim().isEmpty()||p.name.trim().length()>80) throw new IllegalArgumentException("Enter a product name (1–80 characters).");
            SaleMath.checkMoney(p.price); if(p.price==0)throw new IllegalArgumentException("Price must be greater than zero.");
            if(!ToppingImages.accepts(p.image))throw new IllegalArgumentException("Choose a topping image.");
            if(p.bundleSize!=0||p.bundlePrice!=0) SaleMath.total(p.price,p.bundleSize,"Promo",p.bundleSize,p.bundlePrice,0);
            p.name=p.name.trim();
            if(p.id==0)return db.dao().insertProduct(p);
            if(db.dao().updateProduct(p)!=1)throw new IllegalArgumentException("Product no longer exists.");
            return p.id;
        });
    }
    public long saveSale(SaleDraft d) {
        return db.runInTransaction(()->{
            if(d.requestId==null||d.requestId.length()>80||d.requestId.isEmpty())throw new IllegalArgumentException("Start a new sale.");
            Sale original=d.id==0?null:db.dao().sale(d.id);
            if(d.id!=0&&original==null)throw new IllegalArgumentException("Sale no longer exists.");
            if(d.id==0){Sale saved=db.dao().byToken(d.requestId);if(saved!=null)return saved.id;}
            if(!"Cash".equals(d.payment)&&!"GCash".equals(d.payment))throw new IllegalArgumentException("Choose Cash or GCash.");
            SaleMath.validDate(d.day);
            if(d.day.compareTo(SaleMath.today())>0)throw new IllegalArgumentException("A recorded sale cannot have a future date.");
            if(d.notes==null||d.notes.length()>500)throw new IllegalArgumentException("Keep customer / notes within 500 characters.");
            Product p=db.dao().product(d.productId);if(p==null)throw new IllegalArgumentException("Add and choose a product first.");
            Sale s=original==null?new Sale():original;
            if(original==null||original.productId!=p.id){s.productName=p.name;s.unitPrice=p.price;s.bundleSize=p.bundleSize;s.bundlePrice=p.bundlePrice;s.image=p.image;}
            s.productId=p.id;s.mode=d.mode;s.quantity=d.quantity;s.payment=d.payment;s.day=d.day;s.notes=d.notes.trim();
            s.total=SaleMath.total(s.unitPrice,d.quantity,d.mode,s.bundleSize,s.bundlePrice,"Custom".equals(d.mode)?SaleMath.parseMoney(d.customAmount):0);
            s.updatedAt=System.currentTimeMillis();
            if(original==null){s.requestId=d.requestId;s.createdAt=s.updatedAt;return db.dao().insertSale(s);}
            db.dao().updateSale(s);return s.id;
        });
    }
}
