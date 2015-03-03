package com.ggstudios.asm;

import java.util.HashSet;
import java.util.Set;

public class IntermediateSource {
    private static final int NUM_SECTIONS = 3;

    private String fileName;

    private StringBuilder[] sections;
    private Set<String> externs = new HashSet<>();
    private Set<String> labels = new HashSet<>();

    private int activeSectionId = -1;

    public IntermediateSource() {
        sections = new StringBuilder[NUM_SECTIONS];
        sections[Section.BSS] = new StringBuilder();
        sections[Section.DATA] = new StringBuilder();
        sections[Section.TEXT] = new StringBuilder();
    }

    public void setActiveSectionId(int sectionId) {
        activeSectionId = sectionId;
    }

    public void linkLabel(String label) {
        if (!labels.contains(label)) {
            externs.add(label);
        }
    }

    private StringBuilder getActiveSectionId() {
        return sections[activeSectionId];
    }

    public void glabel(String label) {
        labels.add(label);
        getActiveSectionId()
                .append("\tglobal\t")
                .append(label)
                .append('\n')
                .append(label)
                .append(":\n");
    }

    public void label(String label) {
        labels.add(label);
        getActiveSectionId()
                .append(label)
                .append(":\n");
    }

    public void dd(String labelValue) {
        linkLabel(labelValue);
        getActiveSectionId()
                .append("\tdd\t")
                .append(labelValue)
                .append("\n");
    }

    public void dd(int i) {
        getActiveSectionId()
                .append("\tdd\t")
                .append(i)
                .append("\n");
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (String s : externs) {
            sb.append("extern ");
            sb.append(s);
            sb.append("\n");
        }

        sb.append("\nsection .bss\n");
        sb.append(sections[Section.BSS]);
        sb.append("\nsection .text\n");
        sb.append(sections[Section.TEXT]);
        sb.append("\nsection .data\n");
        sb.append(sections[Section.DATA]);

        return sb.toString();
    }

    public void resd(String name) {
        labels.add(name);
        getActiveSectionId()
                .append(name)
                .append(":\t resd 1\n");
    }

    public void ret() {
        getActiveSectionId()
                .append("\tret\n");
    }

    public void mov(String label, int i) {
        getActiveSectionId()
                .append("\tmov \t[")
                .append(label)
                .append("], dword ")
                .append(i)
                .append('\n');
    }


    public void mov(Register r, String complexExpression) {
        getActiveSectionId()
                .append("\tmov \t")
                .append(r.getAsm())
                .append(", ")
                .append(complexExpression)
                .append('\n');
    }

    public void mov(Register r, int val) {
        getActiveSectionId()
                .append("\tmov \t")
                .append(r.getAsm())
                .append(", ")
                .append(val)
                .append('\n');
    }

    public void mov(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tmov \t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void mov(String complexExpression, String complexExpression2) {
        getActiveSectionId()
                .append("\tmov \t")
                .append(complexExpression)
                .append(", ")
                .append(complexExpression2)
                .append('\n');
    }

    public void push(Register r) {
        getActiveSectionId()
                .append("\tpush\t")
                .append(r.getAsm())
                .append('\n');
    }

    public void push(String label) {
        linkLabel(label);

        getActiveSectionId()
                .append("\tpush\t")
                .append(label)
                .append('\n');
    }

    public void pop(Register r) {
        getActiveSectionId()
                .append("\tpop \t")
                .append(r.getAsm())
                .append('\n');
    }

    public void shl(Register r1, int val) {
        getActiveSectionId()
                .append("\tshl \t")
                .append(r1.getAsm())
                .append(", ")
                .append(val)
                .append('\n');
    }

    public void imul(Register r1, Register r2, int val) {
        getActiveSectionId()
                .append("\timul\t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append(", ")
                .append(val)
                .append('\n');
    }

    public void add(Register r1, int val) {
        getActiveSectionId()
                .append("\tadd \t")
                .append(r1.getAsm())
                .append(", ")
                .append(val)
                .append('\n');
    }

    public void extern(String label) {
        externs.add(label);
        labels.add(label);
    }

    public void call(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tcall\t")
                .append(label)
                .append('\n');
    }

    public void db(String label, String s) {
        labels.add(label);
        getActiveSectionId()
                .append(label)
                .append("\tdb\t")
                .append('"')
                .append(s)
                .append("\", 0\n");
    }

    public void addComment(String s) {
        getActiveSectionId()
                .append(';')
                .append(s)
                .append('\n');
    }
}
