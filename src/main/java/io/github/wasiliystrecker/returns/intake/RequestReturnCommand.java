package io.github.wasiliystrecker.returns.intake;

/**
 * Input contract for requesting a return.
 *
 * @param orderReference merchant-facing order reference
 * @param itemReference item or order-line reference
 * @param reason stable reason code such as {@code DAMAGED}
 * @param comment optional applicant comment
 * @param requestedRefundMinorUnits requested refund in the currency's minor units
 * @param currency ISO 4217 currency code
 */
public record RequestReturnCommand(
    String orderReference,
    String itemReference,
    String reason,
    String comment,
    long requestedRefundMinorUnits,
    String currency) {}
