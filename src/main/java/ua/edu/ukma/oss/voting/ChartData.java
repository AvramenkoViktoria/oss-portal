package ua.edu.ukma.oss.voting;

import java.util.List;

/**
 * Renderer-neutral data of a voting chart.
 *
 * @param kind    which view produced the data
 * @param slices  counts per choice, in display order
 * @param cells   one entry per member in seat order; empty for the summary view
 * @param columns suggested number of grid columns for the member view
 */
public record ChartData(Kind kind, List<Slice> slices, List<Cell> cells, int columns) {

    public enum Kind {
        SUMMARY,
        MEMBER_GRID
    }

    public record Slice(VoteChoice choice, int count) {
    }

    public record Cell(Long membershipId, VoteChoice choice) {
    }
}
