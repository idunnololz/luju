package com.ggstudios.types;

import com.ggstudios.utils.ListUtils;

import java.util.ArrayList;
import java.util.List;

public class InterfaceDecl extends TypeDecl {
    private List<String> extendsList = new ArrayList<>();

    public InterfaceDecl() {
        setDeclType("Interface");
    }

    public List<String> getExtendsList() {
        return extendsList;
    }

    public void addExtend(String extend) {
        this.extendsList.add(extend);
    }

    @Override
    protected void toPrettyStringThis(StringBuilder sb, int level) {
        super.toPrettyStringThis(sb, level);
        sb.setLength(sb.length() - 1);  // erase the ')'\
        sb.append("; Extends: [");

        if (extendsList.size() != 0) {
            for (String s : ListUtils.reverse(extendsList)) {
                sb.append(s);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("])");
    }
}
