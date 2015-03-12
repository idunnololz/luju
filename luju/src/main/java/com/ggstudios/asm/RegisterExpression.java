package com.ggstudios.asm;

public class RegisterExpression {
    private boolean unary = true;
    private boolean hasNumber = false;
    private boolean isLabel = false;

    private Register r1, r2;
    private int num;
    private String label;

    private Operator op;

    private void reset() {
        unary = false;
        hasNumber = false;
        isLabel = false;
    }

    public void set(RegisterExpression re) {
        unary = re.unary;
        hasNumber = re.hasNumber;
        isLabel = re.isLabel;
        r1 = re.r1;
        r2 = re.r2;
        num = re.num;
        label = re.label;
        op = re.op;
    }

    public void set(Register r1, Operator op, Register r2) {
        reset();
        unary = false;

        this.r1 = r1;
        this.op = op;
        this.r2 = r2;
    }

    public void set(Register r1, Operator op, int num) {
        reset();
        unary = false;
        hasNumber = true;

        this.r1 = r1;
        this.op = op;
        this.num = num;
    }

    public void set(Register r1) {
        reset();
        unary = true;

        this.r1 = r1;
    }

    public void set(String label) {
        reset();
        unary = true;
        isLabel = true;

        this.label = label;
    }

    public void set(String label, Register r) {
        reset();
        unary = false;
        isLabel = true;

        this.label = label;
        this.r1 = r;
    }

    public boolean isLabel() {
        return isLabel;
    }

    public String toString() {
        if (unary) {
            if (isLabel) {
                return label;
            } else {
                return r1.getAsm();
            }
        } else {
            if (hasNumber) {
                return r1.getAsm() + op + num;
            } else if (isLabel) {
                return String.format("%s[%s]", label, r2.getAsm());
            } else {
                return r1.getAsm() + op + r2.getAsm();
            }
        }
    }

    public boolean isRegisterUsed(Register r) {
        if (unary || hasNumber) {
            return !isLabel && r == r1;
        } else {
            if (isLabel) {
                return r == r1;
            } else {
                return r == r1 || r == r2;
            }
        }
    }

    public boolean isUnary() {
        return unary;
    }

    public String getLabel() {
        return label;
    }

    public void dumpToSingleRegister(IntermediateSource source, Register r) {
        if (!isUnary()) {
            source.lea(r, this);
        } else if (isLabel()) {
            source.mov(r, this.toString());
        } else if (!isRegisterUsed(r)) {
            source.mov(r, this.toString());
        }
    }
}
