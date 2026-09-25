package com.project.taxratesystem.tax.dto;

import com.project.taxratesystem.tax.entity.TaxExample;

/**
 * A worked example of API.md §8.2/§8.4, e.g. {@code ₱600,000} → {@code ₱22,500 + 20% of ₱200,000}
 * = {@code ₱62,500}.
 *
 * <p>All three fields are display strings rendered verbatim by the client.
 */
public class TaxExampleResponse {

    private String amount;
    private String computation;
    private String estimatedTax;

    public TaxExampleResponse() {
    }

    public TaxExampleResponse(String amount, String computation, String estimatedTax) {
        this.amount = amount;
        this.computation = computation;
        this.estimatedTax = estimatedTax;
    }

    public static TaxExampleResponse of(TaxExample example) {
        return new TaxExampleResponse(example.getAmount(), example.getComputation(),
                example.getEstimatedTax());
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getComputation() {
        return computation;
    }

    public void setComputation(String computation) {
        this.computation = computation;
    }

    public String getEstimatedTax() {
        return estimatedTax;
    }

    public void setEstimatedTax(String estimatedTax) {
        this.estimatedTax = estimatedTax;
    }
}

