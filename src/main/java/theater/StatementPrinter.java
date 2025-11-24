package theater;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * This class generates a plain-text statement for a given invoice of performances.
 */
public class StatementPrinter {
    private final Invoice invoice;
    private final Map<String, Play> plays;

    /**
     * Constructs a new StatementPrinter.
     *
     * @param invoice the invoice whose performances will be printed
     * @param plays   a map of play IDs to Play objects
     */
    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    /**
     * Returns a formatted statement of the invoice associated with this printer.
     *
     * @return the formatted statement text
     * @throws RuntimeException if one of the play types is not known
     */
    public String statement() {
        final int totalAmount = getTotalAmount();
        final int volumeCredits = getTotalVolumeCredits();

        final StringBuilder result =
                new StringBuilder("Statement for " + invoice.getCustomer() + System.lineSeparator());

        for (Performance performance : invoice.getPerformances()) {
            result.append(String.format(
                    "  %s: %s (%s seats)%n",
                    getPlay(performance).getName(),
                    usd(getAmount(performance)),
                    performance.getAudience()
            ));
        }

        result.append(String.format("Amount owed is %s%n", usd(totalAmount)));
        result.append(String.format("You earned %s credits%n", volumeCredits));

        return result.toString();
    }

    /**
     * Computes the total volume credits across all performances.
     *
     * @return the total volume credits earned
     */
    private int getTotalVolumeCredits() {
        int result = 0;
        for (Performance performance : invoice.getPerformances()) {
            result += getVolumeCredits(performance);
        }
        return result;
    }

    /**
     * Computes the total amount owed across all performances.
     *
     * @return the total amount in cents
     */
    private int getTotalAmount() {
        int result = 0;
        for (Performance performance : invoice.getPerformances()) {
            result += getAmount(performance);
        }
        return result;
    }

    /**
     * Format an amount (in cents) as a US currency string.
     *
     * @param amountInCents the amount in cents
     * @return the amount formatted in US currency
     */
    private String usd(int amountInCents) {
        final NumberFormat frmt = NumberFormat.getCurrencyInstance(Locale.US);
        return frmt.format(amountInCents / Constants.PERCENT_FACTOR);
    }

    /**
     * Computes the volume credits earned for a single performance.
     *
     * @param performance the performance being evaluated
     * @return volume credits earned for this performance
     */
    private int getVolumeCredits(Performance performance) {
        int result = 0;

        result += Math.max(
                performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD, 0);

        if ("comedy".equals(getPlay(performance).getType())) {
            result += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }

        return result;
    }

    /**
     * Helper: return the play associated with the given performance.
     *
     * @param performance the performance to evaluate
     * @return the associated Play object
     */
    private Play getPlay(Performance performance) {
        return plays.get(performance.getPlayID());
    }

    /**
     * Computes the amount owed for a single performance.
     *
     * @param performance the performance being evaluated
     * @return amount owed (in cents)
     * @throws RuntimeException if the play type is unknown
     */
    private int getAmount(Performance performance) {
        final Play play = getPlay(performance);
        int result;

        switch (play.getType()) {
            case "tragedy":
                result = Constants.TRAGEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.TRAGEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.TRAGEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.TRAGEDY_AUDIENCE_THRESHOLD);
                }
                break;

            case "comedy":
                result = Constants.COMEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                            + Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD);
                }
                result += Constants.COMEDY_AMOUNT_PER_AUDIENCE * performance.getAudience();
                break;

            default:
                throw new RuntimeException(
                        String.format("unknown type: %s", play.getType()));
        }

        return result;
    }
}
