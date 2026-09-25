package com.project.taxratesystem.config;

import com.project.taxratesystem.tax.entity.TaxBracket;
import com.project.taxratesystem.tax.entity.TaxExample;
import com.project.taxratesystem.tax.entity.TaxType;
import com.project.taxratesystem.tax.enums.TaxTypeCode;
import com.project.taxratesystem.tax.repository.TaxTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the ten tax types of API.md §8.6 - the reference data the client renders verbatim and the
 * catalogue every calculation resolves its tax type against.
 *
 * <p>Runs after the Flyway migrations and is idempotent: a type is inserted only when its code is
 * absent, so restarting the application never duplicates rows. The display strings (peso signs,
 * en-dashes, wording) are part of the contract - never "improve" them without updating API.md.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TaxTypeRepository taxTypeRepository;

    public DataSeeder(TaxTypeRepository taxTypeRepository) {
        this.taxTypeRepository = taxTypeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int seeded = 0;
        for (TaxType taxType : catalogue()) {
            if (taxTypeRepository.existsByCode(taxType.getCode())) {
                continue;
            }
            taxTypeRepository.save(taxType);
            seeded++;
        }
        if (seeded > 0) {
            log.info("Seeded {} tax type(s) from API.md §8.6.", seeded);
        }
    }

    private static List<TaxType> catalogue() {
        return List.of(
                seed(TaxTypeCode.PERSONAL_INCOME_TAX,
                        "Tax on an individual's net taxable income under the Philippine TRAIN Law.",
                        "0% – 35%", LocalDate.of(2023, 1, 1),
                        TaxExample.of("₱600,000", "₱22,500 + 20% of ₱200,000", "₱62,500"),
                        TaxBracket.of("₱0 – ₱250,000", "Exempt"),
                        TaxBracket.of("₱250,001 – ₱400,000", "15% of excess over ₱250,000"),
                        TaxBracket.of("₱400,001 – ₱800,000", "₱22,500 + 20% of excess"),
                        TaxBracket.of("₱800,001 – ₱2,000,000", "₱102,500 + 25% of excess"),
                        TaxBracket.of("₱2,000,001 – ₱8,000,000", "₱402,500 + 30% of excess"),
                        TaxBracket.of("Over ₱8,000,000", "₱2,202,500 + 35% of excess")),
                seed(TaxTypeCode.CORPORATE_INCOME_TAX,
                        "Tax on the net taxable income of domestic and resident foreign corporations under the CREATE Law.",
                        "20% – 25%", LocalDate.of(2020, 7, 1),
                        TaxExample.of("₱10,000,000", "₱10,000,000 × 25%", "₱2,500,000"),
                        TaxBracket.of("Domestic corp., net taxable income ≤ ₱5M", "20% (CREATE reduced rate)"),
                        TaxBracket.of("Domestic corp., net taxable income > ₱5M", "25%"),
                        TaxBracket.of("Resident foreign corp.", "25%"),
                        TaxBracket.of("Non-resident foreign corp.", "30%")),
                seed(TaxTypeCode.VAT,
                        "Consumption tax on the sale of goods and services at each stage of the supply chain.",
                        "12%", LocalDate.of(2018, 1, 1),
                        TaxExample.of("₱100,000", "₱100,000 × 12%", "₱12,000"),
                        TaxBracket.of("Sale of goods or properties", "12%"),
                        TaxBracket.of("Sale of services", "12%"),
                        TaxBracket.of("Importation of goods", "12%")),
                seed(TaxTypeCode.PERCENTAGE_TAX,
                        "Business tax on gross sales/receipts of non-VAT taxpayers below the VAT threshold.",
                        "3%", LocalDate.of(2018, 1, 1),
                        TaxExample.of("₱2,000,000", "₱2,000,000 × 3%", "₱60,000"),
                        TaxBracket.of("Gross sales or receipts ≤ ₱3M", "3%"),
                        TaxBracket.of("VAT-registered taxpayers", "Not applicable")),
                seed(TaxTypeCode.CGT_REAL_PROPERTY,
                        "Capital Gains Tax on the sale of Philippine real property held as a capital asset.",
                        "6%", LocalDate.of(1998, 1, 1),
                        TaxExample.of("₱5,000,000", "₱5,000,000 × 6%", "₱300,000"),
                        TaxBracket.of("Selling price or zonal value", "6%"),
                        TaxBracket.of("Higher of the two", "6%"),
                        TaxBracket.of("Sale of principal residence (exemption)", "Exempt")),
                seed(TaxTypeCode.CGT_SHARES,
                        "Capital Gains Tax on shares of stock not traded through the local stock exchange.",
                        "5% – 10%", LocalDate.of(1998, 1, 1),
                        TaxExample.of("₱150,000 net gain", "₱5,000 + 10% of ₱50,000", "₱10,000"),
                        TaxBracket.of("Net gain ≤ ₱100,000", "5%"),
                        TaxBracket.of("Net gain > ₱100,000", "10% of the excess")),
                seed(TaxTypeCode.DOCUMENTARY_STAMP_TAX,
                        "Tax on documents, instruments and papers showing transfer of rights.",
                        "0.5% – 1.5%", LocalDate.of(2005, 1, 1),
                        TaxExample.of("₱2,000,000 deed of sale", "₱2,000,000 × 1.5%", "₱30,000"),
                        TaxBracket.of("Loan agreements", "0.5%"),
                        TaxBracket.of("Deeds of sale", "1.5%"),
                        TaxBracket.of("Lease agreements", "0.5%")),
                seed(TaxTypeCode.WITHHOLDING_TAX,
                        "Tax withheld by the payer of income from the payee at the prescribed rate.",
                        "1% – 15%", LocalDate.of(2018, 1, 1),
                        TaxExample.of("₱100,000 professional fees", "₱100,000 × 10%", "₱10,000"),
                        TaxBracket.of("Professional fees", "10%"),
                        TaxBracket.of("Rentals", "5%"),
                        TaxBracket.of("Commissions", "10%")),
                seed(TaxTypeCode.ESTATE_TAX,
                        "Tax on the right to transmit the estate of a decedent to the lawful heirs.",
                        "6%", LocalDate.of(2018, 1, 1),
                        TaxExample.of("₱15,000,000", "₱15,000,000 − ₱5,000,000 = ₱10,000,000 × 6%", "₱600,000"),
                        TaxBracket.of("Net estate ≤ ₱5M (standard deduction)", "Exempt"),
                        TaxBracket.of("Net estate > ₱5M", "6% of the excess")),
                seed(TaxTypeCode.REAL_PROPERTY_TAX,
                        "Annual ad valorem tax assessed by the LGU on real property.",
                        "1% – 2%", LocalDate.of(1992, 1, 1),
                        TaxExample.of("₱5,000,000 assessed value", "₱5,000,000 × 1%", "₱50,000"),
                        TaxBracket.of("Basic RPT (city or municipality)", "1%"),
                        TaxBracket.of("Basic RPT (province)", "1%"),
                        TaxBracket.of("Additional levy (Special Education Fund)", "Up to 1%"))
        );
    }

    /** One §8.6 row: name from the enum, summary fields plus its bracket table and one example. */
    private static TaxType seed(TaxTypeCode code, String description, String currentRate,
                                LocalDate effectiveDate, TaxExample example, TaxBracket... brackets) {
        TaxType taxType = TaxType.builder()
                .code(code.getCode())
                .name(code.getDisplayName())
                .description(description)
                .currentRate(currentRate)
                .effectiveDate(effectiveDate)
                .build();
        for (TaxBracket bracket : brackets) {
            taxType.addBracket(bracket);
        }
        taxType.addExample(example);
        return taxType;
    }
}

