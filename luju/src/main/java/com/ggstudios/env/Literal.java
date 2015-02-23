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
                type = BaseEnvironment.TYPE_CHAR;
                break;
            case INTLIT:
                value = (int)t.getVal();
                type = BaseEnvironment.TYPE_INT;
                break;
            case FALSE:
                value = false;
                type = BaseEnvironment.TYPE_BOOLEAN;
                break;
            case TRUE:
                value = true;
                type = BaseEnvironment.TYPE_BOOLEAN;
                break;
            case NULL:
                value = null;
                type = BaseEnvironment.TYPE_NULL;
                break;
            default:
                throw new RuntimeException(String.format("Literal type '%s' is not supported",
                        t.getType()));
        }
    }

    public Object getValue() {
        return value;
    }

}
