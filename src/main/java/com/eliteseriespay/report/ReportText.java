package com.eliteseriespay.report;

final class ReportText {

    private ReportText() {
    }

    static String episodesFundedLine(int count) {
        return "Собрано на " + count + " " + pluralSeries(count) + ".";
    }

    static String ordinalEpisode(int number) {
        int mod100 = number % 100;
        int mod10 = number % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return number + "-й";
        }
        return switch (mod10) {
            case 1 -> number + "-й";
            case 2 -> number + "-й";
            case 3 -> number + "-й";
            case 4 -> number + "-й";
            default -> number + "-й";
        };
    }

    private static String pluralSeries(int count) {
        int mod100 = count % 100;
        int mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return "серий";
        }
        return switch (mod10) {
            case 1 -> "серию";
            case 2, 3, 4 -> "серии";
            default -> "серий";
        };
    }
}
