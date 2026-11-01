package com.homebaker.cookiesales;

final class ToppingImages {
    static final String[] KEYS={"chocolate","marshmallows","smores","cadbury","opuff","oreos","creamo"};
    static final String[] LABELS={"Chocolate","Marshmallows","S’mores","Cadbury","O-Puff Marshmallow","Oreos","Cream-O"};
    private static final int[] DRAWABLES={R.drawable.topping_chocolate,R.drawable.topping_marshmallows,R.drawable.topping_smores,R.drawable.topping_cadbury,R.drawable.topping_opuff,R.drawable.topping_oreos,R.drawable.topping_creamo};
    private ToppingImages() { }
    private static int index(String key){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;return -1;}
    static boolean accepts(String key){return "plain".equals(key)||index(key)>=0;}
    static String label(String key){int i=index(key);return i<0?"Choose topping":LABELS[i];}
    // Old plain records remain valid but must not acquire a misleading topping image.
    static int drawable(String key){int i=index(key);return i<0?0:DRAWABLES[i];}
}
