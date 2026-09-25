package com.project.taxratesystem.tax.dto;

import com.project.taxratesystem.tax.entity.TaxBracket;

/**
 * One bracket row of API.md §8.2/§8.3.
 *
 * <p>{@code range} and {@code rate} are display strings on purpose - the client renders them
 * verbatim (e.g. {@code ₱400,001 – ₱800,000} / {@code ₱22,500 + 20% of excess}).
 */
public class TaxBracketResponse {

    private String range;
    private String rate;

    public TaxBracketResponse() {
    }

    public TaxBracketResponse(String range, String rate) {
        this.range = range;
        this.rate = rate;
    }

    public static TaxBracketResponse of(TaxBracket bracket) {
        return new TaxBracketResponse(bracket.getRange(), bracket.getRate());
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }
}

