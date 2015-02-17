package com.ggstudios.types;

import com.ggstudios.env.Class;
import com.ggstudios.luju.Token;

import java.util.List;

public class ReferenceType extends Expression {
    private final String type;
    private boolean isArray;

    private String[] typeArr;

    private Class clazz;

    public ReferenceType(NameVariable nVar, boolean isArray) {
        setType(REFERENCE_TYPE);

        type = nVar.getName();
        List<Token> l = nVar.getIdSeq();
        typeArr = new String[l.size()];
        for (int i = 0; i < typeArr.length; i++) {
           typeArr[i] = l.get(i).getRaw();
        }
        this.isArray = isArray;
    }

    public ReferenceType(String type) {
        setType(REFERENCE_TYPE);

        if (type.endsWith("[]")) {
            this.type = type.substring(0, type.length() - 2);
            isArray = true;
        } else {
            this.type = type;
            isArray = false;
        }
    }

    public ReferenceType(List<Token> type, boolean isArray) {
        setType(REFERENCE_TYPE);

        typeArr = new String[type.size()];
        for (int i = 0; i < typeArr.length; i++) {
            typeArr[i] = type.get(i).getRaw();
        }

        this.isArray = isArray;

        StringBuilder sb = new StringBuilder();
        for (Token t : type) {
            sb.append(t.getRaw());
            sb.append('.');
        }
        sb.setLength(sb.length() - 1);
        this.type = sb.toString();
    }

    public String[] getTypeAsArray() {
        if (typeArr == null) {
            typeArr = type.split("\\.");
        }
        return typeArr;
    }

    public boolean isArray() {
        return isArray;
    }

    @Override
    public String toString() {
        return type;
    }

    public Class getProper() {
        return clazz;
    }

    public void setProper(Class clazz) {
        this.clazz = clazz;
    }
}
