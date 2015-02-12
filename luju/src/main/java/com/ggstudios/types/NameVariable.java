package com.ggstudios.types;

import com.ggstudios.luju.Token;

import java.util.ArrayList;
import java.util.List;

public class NameVariable extends Variable {
    private List<Token> idSeq = new ArrayList<>();

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        for (Token t : idSeq) {
            sb.append(t.getRaw());
            sb.append('.');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public Token getId() {
        return idSeq.get(0);
    }

    public void setId(Token id) {
        if (idSeq.size() == 0) {
            idSeq.add(id);
        } else if (idSeq.size() == 1) {
            idSeq.set(0, id);
        } else {
            throw new IllegalStateException("Attempting to set name on a qualified name.");
        }
    }

    public void addId(Token id) {
        idSeq.add(id);
    }
}
