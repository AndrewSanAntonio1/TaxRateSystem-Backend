package com.project.taxratesystem.tax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.taxratesystem.tax.entity.TaxType;

import java.time.LocalDate;
import java.util.List;

/**
 * A tax type of API.md §8.1 (summary form) and §8.2 (detail form).
 *
 * <p>One class carries both shapes: the summary sets {@code bracketCount} and {@code hasExample},
 * the detail sets {@code brackets} and {@code example}. The half that does not apply to the
 * requested form is omitted from the JSON by {@link JsonInclude} - exactly the two bodies the
 * contract shows.
 *
 * <p>Every display string comes from the seeded rows and is rendered verbatim by the client; never
 * "improve" the wording here.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxResponse {

    private String code;
    private String name;
    private String description;
    private String currentRate;
    private LocalDate effectiveDate;
    private Integer bracketCount;
    private Boolean hasExample;
    private List<TaxBracketResponse> brackets;
    private TaxExampleResponse example;

    /** The §8.1 catalogue row: no brackets and no example body, only their summary flags. */
    public static TaxResponse summaryOf(TaxType taxType) {
        TaxResponse response = base(taxType);
        response.bracketCount = taxType.getBrackets().size();
        response.hasExample = !taxType.getExamples().isEmpty();
        return response;
    }

    /** The §8.2 detail: the full bracket table plus today's (single) worked example. */
    public static TaxResponse detailOf(TaxType taxType) {
        TaxResponse response = base(taxType);
        response.brackets = taxType.getBrackets().stream().map(TaxBracketResponse::of).toList();
        response.example = taxType.getExamples().stream().findFirst()
                .map(TaxExampleResponse::of)
                .orElse(null);
        return response;
    }

    private static TaxResponse base(TaxType taxType) {
        TaxResponse response = new TaxResponse();
        response.code = taxType.getCode();
        response.name = taxType.getName();
        response.description = taxType.getDescription();
        response.currentRate = taxType.getCurrentRate();
        response.effectiveDate = taxType.getEffectiveDate();
        return response;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrentRate() {
        return currentRate;
    }

    public void setCurrentRate(String currentRate) {
        this.currentRate = currentRate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Integer getBracketCount() {
        return bracketCount;
    }

    public void setBracketCount(Integer bracketCount) {
        this.bracketCount = bracketCount;
    }

    public Boolean getHasExample() {
        return hasExample;
    }

    public void setHasExample(Boolean hasExample) {
        this.hasExample = hasExample;
    }

    public List<TaxBracketResponse> getBrackets() {
        return brackets;
    }

    public void setBrackets(List<TaxBracketResponse> brackets) {
        this.brackets = brackets;
    }

    public TaxExampleResponse getExample() {
        return example;
    }

    public void setExample(TaxExampleResponse example) {
        this.example = example;
    }
}

