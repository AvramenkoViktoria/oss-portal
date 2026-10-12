package ua.edu.ukma.oss.voting;

import org.springframework.stereotype.Component;

/**
 * Creates the chart views. Views are cheap and stateless, so callers may create them per request.
 */
@Component
public class VoteViewFactory {

    /**
     * Member-by-member grid sized for a body of the given size.
     */
    public VoteView create(int bodySize) {
        return new MemberGridView(bodySize);
    }

    /**
     * Totals per choice.
     */
    public VoteView summary() {
        return new SummaryChartView();
    }
}
