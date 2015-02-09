package com.ggstudios.utils;

public class Print {
    private static Printer printer = new DefaultPrinter();

    public static void ln(String s) {
        printer.printLn(s);
    }

    public static void p(String s) {
        printer.print(s);
    }

    public static void e(String s) {
        printer.printErr(s);
    }

    public static void setPrinter(Printer p) {
        printer = p;
    }

    public static abstract class Printer {
        public abstract void printLn(String s);
        public abstract void print(String s);
        public abstract void printErr(String s);
    }

    public static class DefaultPrinter extends Printer {

        @Override
        public void printLn(String s) {
            System.out.println(s);
        }

        @Override
        public void print(String s) {
            System.out.print(s);
        }

        @Override
        public void printErr(String s) {
            System.err.println(s);
        }
    }

    public static class QuietPrinter extends Printer {

        @Override
        public void printLn(String s) {

        }

        @Override
        public void print(String s) {

        }

        @Override
        public void printErr(String s) {
            System.err.println(s);
        }
    }

    public static class SilentPrinter extends Printer {

        @Override
        public void printLn(String s) {

        }

        @Override
        public void print(String s) {

        }

        @Override
        public void printErr(String s) {

        }
    }
}
