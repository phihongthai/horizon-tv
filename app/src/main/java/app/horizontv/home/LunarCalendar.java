package app.horizontv.home;

/** Vietnamese lunar calendar (UTC+7), Ho Ngoc Duc algorithm. */
final class LunarCalendar {

    private static final double TZ = 7.0;
    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};

    private LunarCalendar() {
    }

    static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        }
        return jd;
    }

    private static int newMoonDay(int k) {
        double T = k / 1236.85;
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = Math.PI / 180;
        double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        Jd1 = Jd1 + 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
        C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
        C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
        C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
        C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
        C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
        C1 = C1 + 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
        double deltat;
        if (T < -11) {
            deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3;
        } else {
            deltat = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        double jdNew = Jd1 + C1 - deltat;
        return (int) (jdNew + 0.5 + TZ / 24);
    }

    private static int sunLongitude(int jdn) {
        double T = (jdn - 2451545.5 - TZ / 24) / 36525;
        double T2 = T * T;
        double dr = Math.PI / 180;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
        DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290 * Math.sin(dr * 3 * M);
        double L = (L0 + DL) * dr;
        L = L - Math.PI * 2 * ((int) (L / (Math.PI * 2)));
        return (int) (L / Math.PI * 6);
    }

    private static int lunarMonth11(int yy) {
        double off = jdFromDate(31, 12, yy) - 2415021.076998695;
        int k = (int) (off / 29.530588853);
        int nm = newMoonDay(k);
        if (sunLongitude(nm) >= 9) {
            nm = newMoonDay(k - 1);
        }
        return nm;
    }

    private static int leapMonthOffset(int a11) {
        int k = (int) ((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int last;
        int i = 1;
        int arc = sunLongitude(newMoonDay(k + i));
        do {
            last = arc;
            i++;
            arc = sunLongitude(newMoonDay(k + i));
        } while (arc != last && i < 14);
        return i - 1;
    }

    /** Returns {day, month, year, leap(0/1)} in the lunar calendar. */
    static int[] solarToLunar(int dd, int mm, int yy) {
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = (int) ((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = newMoonDay(k + 1);
        if (monthStart > dayNumber) {
            monthStart = newMoonDay(k);
        }
        int a11 = lunarMonth11(yy);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = lunarMonth11(yy - 1);
        } else {
            lunarYear = yy + 1;
            b11 = lunarMonth11(yy + 1);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = (monthStart - a11) / 29;
        int leap = 0;
        int lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapDiff = leapMonthOffset(a11);
            if (diff >= leapDiff) {
                lunarMonth = diff + 10;
                if (diff == leapDiff) {
                    leap = 1;
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth -= 12;
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1;
        }
        return new int[]{lunarDay, lunarMonth, lunarYear, leap};
    }

    static String yearName(int lunarYear) {
        return CAN[(lunarYear + 6) % 10] + " " + CHI[(lunarYear + 8) % 12];
    }

    private static final String[] ANIMAL = {"Rat", "Ox", "Tiger", "Rabbit", "Dragon", "Snake",
            "Horse", "Goat", "Monkey", "Rooster", "Dog", "Pig"};

    /** Example: "Lunar 12/9 · Year of the Horse" (adds "leap" for a leap month). */
    static String describe(int dd, int mm, int yy) {
        int[] l = solarToLunar(dd, mm, yy);
        return "Lunar " + l[0] + "/" + l[1] + (l[3] == 1 ? " (leap)" : "")
                + " \u00B7 Year of the " + ANIMAL[(l[2] + 8) % 12];
    }
}
