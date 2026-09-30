package io.github.rajumark.hoverfly.hideout

/** The kinds of personal information Hideout finds. The order matches the model's labels. */
public enum class PiiType {
    /** Person names (first, last or full). */
    NAME,

    /** Phone numbers, Indian and international. */
    PHONE,

    EMAIL,

    /** UPI IDs, such as `raju@okaxis`. */
    UPI,

    /** Aadhaar numbers and Virtual IDs, also partly masked ones. */
    AADHAAR,

    /** PAN card numbers. */
    PAN,

    /** Credit and debit card numbers, also partly masked ones and last-4 digits in context. */
    CARD,

    /** Bank account numbers and IBANs. */
    BANK_ACCOUNT,

    /** IFSC, SWIFT / BIC and routing codes. */
    IFSC,

    /** Street, house / flat, building and PIN / ZIP code. A city on its own is not hidden. */
    ADDRESS,

    PASSPORT,

    /** EPIC voter ID. */
    VOTER_ID,

    DRIVING_LICENSE,

    /** Vehicle registration numbers and VINs. */
    VEHICLE,

    /** Other government and tax IDs: GSTIN, SSN, national insurance numbers and similar. */
    GOV_ID,

    /** Dates of birth. Other dates are left alone. */
    DOB,

    /** Passwords, OTPs, PINs and CVVs. */
    SECRET,

    /** Usernames and social media handles. */
    USERNAME,

    /** IP addresses. */
    IP,
}

/**
 * One piece of personal information found in a text.
 *
 * @property type what kind it is.
 * @property start index of the first char in the text (UTF-16, like [String.substring]).
 * @property end index after the last char.
 * @property text the found text itself, `text.substring(start, end)`.
 * @property score the model's confidence that this is personal information, 0..1.
 */
public data class Pii(
    val type: PiiType,
    val start: Int,
    val end: Int,
    val text: String,
    val score: Float,
)
