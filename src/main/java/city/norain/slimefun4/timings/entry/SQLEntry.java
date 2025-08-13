package city.norain.slimefun4.timings.entry;

public class SQLEntry implements TimingEntry {
    private final String sql;

    public SQLEntry(String sql) {
        this.sql = sql;
    }

    @Override
    public String getIdentifier() {
        return sql.isBlank() ? null : sql.split(" ")[0];
    }

    @Override
    public String normalize() {
        return sql;
    }

    @Override
    public int hashCode() {
        return sql.hashCode();
    }
}
