package com.ggstudios.asm;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class IntermediateSource {
    private static final int NUM_SECTIONS = 3;

    private String fileName;

    private StringBuilder[] sections;
    private Set<String> externs = new HashSet<>();
    private Set<String> labels = new HashSet<>();

    private Stack<Integer> savePoints = new Stack<>();

    private int activeSectionId = -1;

    private int localLabelCount = 0;

    private int bpOffset = 0;

    private int lastLength = 0;
    private boolean changed = true;

    public IntermediateSource() {
        sections = new StringBuilder[NUM_SECTIONS];
        sections[Section.BSS] = new StringBuilder();
        sections[Section.DATA] = new StringBuilder();
        sections[Section.TEXT] = new StringBuilder();
    }

    public void pushPoint() {
        savePoints.push(getActiveSectionId().length());
    }

    public int popPoint() {
        return savePoints.pop();
    }

    public void restoreTo(int length) {
        getActiveSectionId().setLength(length);
    }

    public void record() {
        lastLength = getActiveSectionId().length();
    }

    public void stop() {
        changed = lastLength != getActiveSectionId().length();
    }

    public void restorePointIfClean() {
        int p = popPoint();

        if (!changed) {
            restoreTo(p);
        }
    }

    public void resetBpOffset() {
        bpOffset = 0;
    }

    public int getBpOffset() {
        return bpOffset;
    }

    public String getFreshLabel() {
        return ".l" + localLabelCount++;
    }

    public void setActiveSectionId(int sectionId) {
        activeSectionId = sectionId;
    }

    public void linkLabel(String label) {
        if (label.charAt(0) == '.') return;

        if (!labels.contains(label)) {
            externs.add(label);
        }
    }

    private StringBuilder getActiveSectionId() {
        return sections[activeSectionId];
    }

    public void global(String label) {
        labels.add(label);
        getActiveSectionId()
                .append("\tglobal\t")
                .append(label)
                .append('\n');
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
        bpOffset -= 4;
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

    public void movRef(RegisterExpression r1, Register r2) {
        if (r1.isLabel()) {
            linkLabel(r1.getLabel());
        }

        getActiveSectionId()
                .append("\tmov \tdword [")
                .append(r1.toString())
                .append("], ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void movRef(Register r1, RegisterExpression r2) {
        if (r2.isLabel()) {
            linkLabel(r2.getLabel());
        }

        getActiveSectionId()
                .append("\tmov \t")
                .append(r1.getAsm())
                .append(", dword [")
                .append(r2.toString())
                .append("]\n");
    }

    public void movRef(RegisterExpression r1, int val) {
        if (r1.isLabel()) {
            linkLabel(r1.getLabel());
        }

        getActiveSectionId()
                .append("\tmov \tdword [")
                .append(r1.toString())
                .append("], ")
                .append(val)
                .append('\n');
    }

    public void movRef(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tmov \t[")
                .append(r1.getAsm())
                .append("], ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void push(Register r) {
        bpOffset += 4;
        getActiveSectionId()
                .append("\tpush\t")
                .append(r.getAsm())
                .append('\n');
    }

    public void push(String label) {
        bpOffset += 4;
        linkLabel(label);

        getActiveSectionId()
                .append("\tpush\t")
                .append(label)
                .append('\n');
    }

    public void pop(Register r) {
        bpOffset -= 4;
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

    public void imul(Register r1, Register r2) {
        getActiveSectionId()
                .append("\timul\t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }


    public void idiv(Register r2) {
        getActiveSectionId()
                .append("\tidiv\t")
                .append(r2.getAsm())
                .append('\n');
    }

    public void add(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tadd \t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
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

    public void sub(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tsub \t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void sub(Register r1, int val) {
        getActiveSectionId()
                .append("\tsub \t")
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
        bpOffset += 4;
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

    public void cmp(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tcmp \t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void cmp(Register r1, int val) {
        getActiveSectionId()
                .append("\tcmp \t")
                .append(r1.getAsm())
                .append(", ")
                .append(val)
                .append('\n');
    }

    public void jge(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tjge \t")
                .append(label)
                .append('\n');
    }

    public void jg(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tjg \t")
                .append(label)
                .append('\n');
    }

    public void jle(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tjle \t")
                .append(label)
                .append('\n');
    }

    public void jl(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tjl \t")
                .append(label)
                .append('\n');
    }

    public void je(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tje \t")
                .append(label)
                .append('\n');
    }

    public void jne(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tjne \t")
                .append(label)
                .append('\n');
    }

    public void jmp(String label) {
        linkLabel(label);
        getActiveSectionId()
                .append("\tjmp \t")
                .append(label)
                .append('\n');
    }

    public void test(Register r1, Register r2) {
        getActiveSectionId()
                .append("\ttest\t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void or(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tor  \t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void and(Register r1, Register r2) {
        getActiveSectionId()
                .append("\tand \t")
                .append(r1.getAsm())
                .append(", ")
                .append(r2.getAsm())
                .append('\n');
    }

    public void cdq() {
        getActiveSectionId()
                .append("\tcdq\n");
    }

    public void lea(Register r1, RegisterExpression r2) {
        if (r2.isLabel()) {
            linkLabel(r2.getLabel());
        }

        getActiveSectionId()
                .append("\tlea \t")
                .append(r1.getAsm())
                .append(", [")
                .append(r2.toString())
                .append("]\n");
    }

    public void lea(Register r1, String complexExpression) {
        getActiveSectionId()
                .append("\tlea \t")
                .append(r1.getAsm())
                .append(", [")
                .append(complexExpression)
                .append("]\n");
    }
}
