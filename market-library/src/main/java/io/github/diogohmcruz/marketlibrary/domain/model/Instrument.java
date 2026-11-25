package io.github.diogohmcruz.marketlibrary.domain.model;

import lombok.experimental.UtilityClass;

/**
 * An instrument is a ticker traded on a venue (exchange):
 * <ul>
 *     <li>The same stock listed on two venues has two independent order books.</li>
 * </ul>
 * The instrument key ({@code venue:ticker}) is what orders are partitioned and matched by.
 */
@UtilityClass
public final class Instrument {
    /** Venue used when none is given; keeps single-exchange clients working unchanged. */
    public static final String DEFAULT_VENUE = "MAIN";

    public static String key(String venue, String ticker) {
        return venue + ":" + ticker;
    }
}
