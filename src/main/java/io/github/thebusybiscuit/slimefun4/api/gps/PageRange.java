package io.github.thebusybiscuit.slimefun4.api.gps;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents a calculated, immutable page window over a linear collection.
 * <p>
 * A {@code PageRange} encapsulates the zero-based half-open index range
 * {@link #from} (inclusive) to {@link #to} (exclusive) that should be displayed
 * for a given page, along with the resolved 1-based current {@link #page}
 * number and the total number of pages {@link #totalPages}.
 * <p>
 * This utility is intended for use in GUIs and APIs that paginate lists, such as
 * waypoint lists or transmitter panels, where callers need a safe, clamped
 * window even when the requested page or sizes are out of bounds.
 *
 */
public class PageRange {
    private final int from;
    private final int to;
    private final int totalPages;
    private final int page;

    public PageRange(int from, int to, int totalPages, int page) {
        this.from = from;
        this.to = to;
        this.totalPages = totalPages;
        this.page = page;
    }

    @ParametersAreNonnullByDefault
    public static @Nonnull PageRange compute(int total, int pageSize, int currentPage) {
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);

        int page = currentPage;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);

        return new PageRange(from, to, totalPages, page);
    }

    /**
     * This method returns the zero-based start index (inclusive) of this page range.
     *
     * @return The start index for this page
     */
    public int getFromIndex() {
        return from;
    }

    /**
     * This method returns the zero-based end index (exclusive) of this page range.
     * The range {@code [from, to)} contains all items shown on this page.
     *
     * @return The end index for this page
     */
    public int getToIndex() {
        return to;
    }

    /**
     * This method returns the total amount of pages calculated for this page range.
     * The returned value will always be at least {@code 1}.
     *
     * @return The total amount of pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * This method returns the current page index after clamping the
     * requested page into a valid range.
     *
     * @return The current page index
     */
    public int getCurrentPage() {
        return page;
    }
}
