package com.ggstudios.env;

import com.ggstudios.luju.Token;

public class Literal extends Field {

    private Object value;

    public Literal(Token t, Environment env) {
        switch (t.getType()) {
            case STRINGLIT:
                value = t.getRaw();
                type = env.lookupClazz("String", false);
                break;
            case CHARLIT:
                value = t.getRaw().charAt(0);
                type = env.lookupClazz("char", false);
                break;
            case INTLIT:
                value = (int)t.getVal();
                type = env.lookupClazz("int", false);
                break;
        }
    }

}
