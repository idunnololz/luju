package com.ggstudios.utils;

import com.ggstudios.error.WeedException;
import com.ggstudios.luju.Parser;
import com.ggstudios.luju.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class ParserUtils {
    public static List<Token> getTokensInTree(Parser.Node tree) {
        List<Token> toks = new ArrayList<>();
        Stack<Parser.Node> toVisit = new Stack<>();
        toVisit.push(tree);

        while (!toVisit.isEmpty()) {
            Parser.Node n = toVisit.pop();
            if (n.t == null) {
                for (Parser.Node child : ListUtils.reverse(n.children)) {
                    toVisit.push(child);
                }
            } else {
                toks.add(n.t);
            }
        }

        return toks;
    }

    public static List<Token> getIdSeqInTree(Parser.Node tree) {
        List<Token> toks = new ArrayList<>();
        Stack<Parser.Node> toVisit = new Stack<>();
        toVisit.push(tree);

        while (!toVisit.isEmpty()) {
            Parser.Node n = toVisit.pop();
            if (n.t == null) {
                for (Parser.Node child : ListUtils.reverse(n.children)) {
                    toVisit.push(child);
                }
            } else {
                if (n.t.getType() != Token.Type.DOT) {
                    toks.add(n.t);
                }
            }
        }

        return toks;
    }
}
