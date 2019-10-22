package com.xlilium.perf.qa;


public class App {

    public static void main(String[] args) {
        new App().run(args);
    }

    private void run(String[] args) {
        JMeterLogAnalyzer jMeterLogAnalyzer = new JMeterLogAnalyzer();
        jMeterLogAnalyzer.run(args);
    }
}
