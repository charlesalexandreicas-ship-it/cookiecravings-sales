package com.homebaker.cookiesales;
import java.io.Serializable;
import java.util.UUID;

public class SaleDraft implements Serializable {
    private static final long serialVersionUID=1L;
    public long id,productId;
    public String requestId=UUID.randomUUID().toString();
    public int quantity=1;
    public String mode="Regular",payment="Cash",day=SaleMath.today(),notes="",customAmount="";
    public static SaleDraft from(Sale s) {
        SaleDraft d=new SaleDraft(); d.id=s.id; d.requestId=s.requestId; d.productId=s.productId; d.quantity=s.quantity;
        d.mode=s.mode; d.payment=s.payment; d.day=s.day; d.notes=s.notes; d.customAmount=SaleMath.inputMoney(s.total); return d;
    }
}
