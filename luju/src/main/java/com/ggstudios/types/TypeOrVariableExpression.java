package com.ggstudios.types;

import com.ggstudios.env.Field;
import com.ggstudios.luju.Token;

import java.util.List;

public class TypeOrVariableExpression extends Expression {
    private String[] stringArr;
    private String string;
    private List<Field> proper;

    public TypeOrVariableExpression(List<Token> type) {
        setType(TYPE_OR_VARIABLE_EXPRESSION);

        stringArr = new String[type.size()];
        for (int i = 0; i < stringArr.length; i++) {
            stringArr[i] = type.get(i).getRaw();
        }

        StringBuilder sb = new StringBuilder();
        for (Token t : type) {
            sb.append(t.getRaw());
            sb.append('.');
        }
        sb.setLength(sb.length() - 1);
        this.string = sb.toString();
    }

    public String[] getTypeAsArray() {
        return stringArr;
    }

    @Override
    public String toString() {
        return string;
    }

    public void setProper(List<Field> proper) {
        this.proper = proper;
    }

    public List<Field> getProper() {
        return proper;
    }
}
