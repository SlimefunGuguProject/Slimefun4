package io.github.thebusybiscuit.slimefun4.api.gps;

public class PageRange {
    final int from;
    final int to;
    final int totalPages;
    final int page;

    public PageRange(int from, int to, int totalPages, int page) {
        this.from = from;
        this.to = to;
        this.totalPages = totalPages;
        this.page = page;
    }

    public static PageRange compute(int total, int pageSize, int currentPage) {
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);

        int page = currentPage;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);

        return new PageRange(from, to, totalPages, page);
    }
}
