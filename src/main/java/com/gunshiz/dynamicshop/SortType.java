package com.gunshiz.dynamicshop;

public enum SortType {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    STOCK_DESC("Stock (High to Low)"),
    STOCK_ASC("Stock (Low to High)"),
    PRICE_DESC("Price (High to Low)"),
    PRICE_ASC("Price (Low to High)");

    private final String displayName;

    SortType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SortType next() {
        SortType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
