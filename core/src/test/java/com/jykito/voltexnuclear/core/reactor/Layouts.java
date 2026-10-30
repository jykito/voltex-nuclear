package com.jykito.voltexnuclear.core.reactor;

/**
 * Layouts for the tests in the letters of the reactor mock-up ({@link Scheme#CODE}): seven rows of seven letters, '.' for
 * an empty cell; the chambers as the sides "LRTB" they open.
 */
final class Layouts {

    private Layouts() {}

    static Zone zone(String chambers, String... rows) {
        return new Scheme("test", chambers, 1, rows).zone();
    }

    /** The mock-up preset "first": era 4, 5 x 5, low enriched rods round graphite, sinks and exchangers, one control rod. */
    static Zone first() {
        return Scheme.FIRST.zone();
    }

    /**
     * The mock-up preset "e5" the tests were written on (the scheme "powerful" until stage 14): era 5, 7 x 7, dual high
     * enriched rods, four control rods, a channel in the middle.
     */
    static Zone powerful() {
        return zone("LRTB", "SXSXSXS", "XHGHGHX", "SGCGCGS", "XHGcGHX", "SGCGCGS", "XHGHGHX", "SXSXSXS");
    }
}
