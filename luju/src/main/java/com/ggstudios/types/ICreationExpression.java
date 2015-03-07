package com.ggstudios.types;

import com.ggstudios.env.Constructor;

import java.util.List;

/**
 * Class instance creation...
 * Of the form: NEW classType LPAREN optArgumentList RPAREN
 */
public class ICreationExpression extends Expression {
    private ReferenceType type;
    private List<Expression> argList;
    private Constructor proper;

    public ICreationExpression() {
        setType(ICREATION_EXPRESSION);
    }

    public List<Expression> getArgList() {
        return argList;
    }

    public void setArgList(List<Expression> argList) {
        this.argList = argList;
    }

    public ReferenceType getType() {
        return type;
    }

    public void setType(ReferenceType type) {
        this.type = type;
    }

    public void setProper(Constructor proper) {
        this.proper = proper;
    }

    public Constructor getProper() {
        return proper;
    }
}
