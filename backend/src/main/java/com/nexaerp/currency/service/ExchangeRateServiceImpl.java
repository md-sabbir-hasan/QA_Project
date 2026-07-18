package com.nexaerp.currency.service;

import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.currency.dto.CurrencyConversionDto;
import com.nexaerp.currency.dto.ExchangeRateRequestDto;
import com.nexaerp.currency.dto.ExchangeRateResponseDto;
import com.nexaerp.currency.entity.Currency;
import com.nexaerp.currency.entity.ExchangeRate;
import com.nexaerp.currency.repository.CurrencyRepository;
import com.nexaerp.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final int RATE_SCALE = 8;
    private static final int AMOUNT_SCALE = 2;

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;

    @Override
    public ExchangeRateResponseDto create(
            ExchangeRateRequestDto request
    ) {

        Currency fromCurrency =
                getActiveCurrency(request.getFromCurrency());

        Currency toCurrency =
                getActiveCurrency(resolveToCurrency(request));

        validateCurrencyPair(fromCurrency, toCurrency);

        if (exchangeRateRepository
                .existsByFromCurrencyIdAndToCurrencyIdAndEffectiveDate(
                        fromCurrency.getId(),
                        toCurrency.getId(),
                        request.getEffectiveDate()
                )) {
            throw new BusinessRuleException(
                    "Exchange rate already exists for "
                            + fromCurrency.getCode()
                            + " to "
                            + toCurrency.getCode()
                            + " on "
                            + request.getEffectiveDate()
            );
        }

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(normalizeRate(request.getRate()))
                .effectiveDate(request.getEffectiveDate())
                .source(request.getSource())
                .build();

        return toResponse(
                exchangeRateRepository.save(exchangeRate)
        );
    }

    @Override
    public ExchangeRateResponseDto update(
            Long id,
            ExchangeRateRequestDto request
    ) {

        ExchangeRate exchangeRate = getEntity(id);

        Currency fromCurrency =
                getActiveCurrency(request.getFromCurrency());

        Currency toCurrency =
                getActiveCurrency(resolveToCurrency(request));

        validateCurrencyPair(fromCurrency, toCurrency);

        exchangeRateRepository
                .findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        fromCurrency.getCode(),
                        toCurrency.getCode(),
                        request.getEffectiveDate()
                )
                .filter(existing ->
                        existing.getEffectiveDate()
                                .equals(request.getEffectiveDate())
                                && !existing.getId().equals(id)
                )
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "Exchange rate already exists for this date"
                    );
                });

        exchangeRate.setFromCurrency(fromCurrency);
        exchangeRate.setToCurrency(toCurrency);
        exchangeRate.setRate(normalizeRate(request.getRate()));
        exchangeRate.setEffectiveDate(request.getEffectiveDate());
        exchangeRate.setSource(request.getSource());

        return toResponse(
                exchangeRateRepository.save(exchangeRate)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getLatestRate(
            String fromCurrency,
            String toCurrency
    ) {

        if (sameCurrency(fromCurrency, toCurrency)) {
            return buildSameCurrencyRate(
                    fromCurrency,
                    LocalDate.now()
            );
        }

        return exchangeRateRepository
                .findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseOrderByEffectiveDateDesc(
                        normalize(fromCurrency),
                        normalize(toCurrency)
                )
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No exchange rate found from "
                                + fromCurrency
                                + " to "
                                + toCurrency
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getRateForDate(
            String fromCurrency,
            String toCurrency,
            LocalDate date
    ) {

        LocalDate rateDate =
                date != null ? date : LocalDate.now();

        if (sameCurrency(fromCurrency, toCurrency)) {
            return buildSameCurrencyRate(
                    fromCurrency,
                    rateDate
            );
        }

        return exchangeRateRepository
                .findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        normalize(fromCurrency),
                        normalize(toCurrency),
                        rateDate
                )
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No exchange rate found from "
                                + fromCurrency
                                + " to "
                                + toCurrency
                                + " on or before "
                                + rateDate
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getHistory(
            String fromCurrency,
            String toCurrency
    ) {
        return exchangeRateRepository
                .findByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseOrderByEffectiveDateDesc(
                        normalize(fromCurrency),
                        normalize(toCurrency)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getAll() {
        return exchangeRateRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRateValue(
            String fromCurrency,
            String toCurrency,
            LocalDate date
    ) {

        if (sameCurrency(fromCurrency, toCurrency)) {
            return BigDecimal.ONE;
        }

        return getRateForDate(
                fromCurrency,
                toCurrency,
                date
        ).getRate();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal convertAmount(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            LocalDate date
    ) {

        validateAmount(amount);

        BigDecimal rate = getRateValue(
                fromCurrency,
                toCurrency,
                date
        );

        return amount.multiply(rate)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyConversionDto convert(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            LocalDate date
    ) {

        LocalDate rateDate =
                date != null ? date : LocalDate.now();

        BigDecimal rate = getRateValue(
                fromCurrency,
                toCurrency,
                rateDate
        );

        BigDecimal convertedAmount =
                amount.multiply(rate)
                        .setScale(
                                AMOUNT_SCALE,
                                RoundingMode.HALF_UP
                        );

        return CurrencyConversionDto.builder()
                .fromCurrency(normalize(fromCurrency))
                .toCurrency(normalize(toCurrency))
                .originalAmount(amount)
                .exchangeRate(rate)
                .convertedAmount(convertedAmount)
                .effectiveDate(rateDate)
                .build();
    }

    @Override
    public void delete(Long id) {
        exchangeRateRepository.delete(getEntity(id));
    }

    private Currency getActiveCurrency(String code) {

        Currency currency = currencyRepository
                .findByCodeIgnoreCase(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Currency not found: " + code
                ));

        if (!Boolean.TRUE.equals(currency.getActive())) {
            throw new BusinessRuleException(
                    "Currency is inactive: " + code
            );
        }

        return currency;
    }

    private ExchangeRate getEntity(Long id) {
        return exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exchange rate not found with id: " + id
                ));
    }

    private void validateCurrencyPair(
            Currency fromCurrency,
            Currency toCurrency
    ) {
        if (fromCurrency.getId().equals(toCurrency.getId())) {
            throw new BusinessRuleException(
                    "From currency and to currency cannot be same"
            );
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "Amount cannot be negative"
            );
        }
    }

    private String resolveToCurrency(
            ExchangeRateRequestDto request
    ) {

        if (request.getToCurrency() != null
                && !request.getToCurrency().isBlank()) {
            return request.getToCurrency();
        }

        return currencyRepository.findByBaseCurrencyTrue()
                .map(Currency::getCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Base currency is not configured"
                ));
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        return rate.setScale(
                RATE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private boolean sameCurrency(
            String fromCurrency,
            String toCurrency
    ) {
        return normalize(fromCurrency)
                .equals(normalize(toCurrency));
    }

    private String normalize(String currencyCode) {
        if (currencyCode == null
                || currencyCode.isBlank()) {
            throw new BusinessRuleException(
                    "Currency code is required"
            );
        }

        return currencyCode.trim().toUpperCase();
    }

    private ExchangeRateResponseDto buildSameCurrencyRate(
            String currencyCode,
            LocalDate date
    ) {
        String code = normalize(currencyCode);

        return ExchangeRateResponseDto.builder()
                .fromCurrency(code)
                .toCurrency(code)
                .rate(BigDecimal.ONE)
                .effectiveDate(date)
                .build();
    }

    private ExchangeRateResponseDto toResponse(
            ExchangeRate exchangeRate
    ) {
        return ExchangeRateResponseDto.builder()
                .id(exchangeRate.getId())
                .fromCurrency(
                        exchangeRate.getFromCurrency().getCode()
                )
                .toCurrency(
                        exchangeRate.getToCurrency().getCode()
                )
                .rate(exchangeRate.getRate())
                .effectiveDate(exchangeRate.getEffectiveDate())
                .source(exchangeRate.getSource())
                .createdAt(exchangeRate.getCreatedAt())
                .updatedAt(exchangeRate.getUpdatedAt())
                .build();
    }
}