package ua.edu.ukma.oss.voting.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteResult;

/**
 * Finds the voting sections of a protocol. Each starts with "Результати голосування" and states the
 * totals as "За - 10", "Проти - 0", "Утримався - 0", "Не голосували - 2" or, shortened, "5 — за".
 * A total may be followed by the names of those who voted that way: "За - 3: Іваненко Іван; Петренко Ірина;
 * Сидоренко Олег. Проти - 0".
 * The question is taken from "Поставили на голосування:" or from the numbered "Слухали:" paragraph.
 */
final class ProtocolVotingParser {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern PAGE_HEADER = Pattern.compile("(?m)^\\s*\\d+\\s*/\\s*\\d+\\s*-.*$");
    private static final Pattern MARKER = Pattern.compile("Результат\\p{L}*\\s+голосування\\s*:?", FLAGS);
    private static final Pattern PUT_TO_VOTE = Pattern.compile("Поставили\\s+на\\s+голосування\\s*:?", FLAGS);
    private static final Pattern NUMBERED_ITEM = Pattern.compile("\\d+\\)\\s*Слухали\\s*:?", FLAGS);
    private static final Pattern REGION_END = Pattern.compile("Рішення|Слухали|Вирішили", FLAGS);
    private static final Pattern PRESENT = Pattern.compile("Присутніх\\s+делегатів[^:\\n]*:\\s*(\\d+)", FLAGS);
    private static final Pattern REJECTED = Pattern.compile("Рішення\\s+не\\s+прийнято", FLAGS);
    private static final Pattern ACCEPTED = Pattern.compile("Рішення\\s+прийнято", FLAGS);
    private static final Pattern FIRST_SENTENCE = Pattern.compile("^(.*?)(?:\\.(?:\\s|$)|$)");
    private static final Pattern ITEM_PREFIX = Pattern.compile(
            "^(?:пункт\\p{L}*\\s+)?(?:порядку\\s+денного\\s+)?(?:про\\s+)?", FLAGS);

    private static final Pattern FOR = counter("[Зз]а(?![\\p{L}])");
    private static final Pattern AGAINST = counter("[Пп]роти(?![\\p{L}])");
    private static final Pattern ABSTAIN = counter("[Уу]трим\\p{L}*");
    private static final Pattern ABSENT = counter("[Нн]е\\s+голосува\\p{L}*");

    private static final String NEXT_LABEL = "(?=\\s(?:За|Проти|Утрим\\p{L}*|Не\\s+голосува\\p{L}*)(?![\\p{L}])\\s*[-—–:]|$)";
    private static final Pattern FOR_NAMES = names("[Зз]а(?![\\p{L}])");
    private static final Pattern AGAINST_NAMES = names("[Пп]роти(?![\\p{L}])");
    private static final Pattern ABSTAIN_NAMES = names("[Уу]трим\\p{L}*");
    private static final Pattern ABSENT_NAMES = names("[Нн]е\\s+голосува\\p{L}*");

    private static final int MAX_QUESTION = 500;
    private static final int MAX_REGION = 3000;

    private ProtocolVotingParser() {
    }

    /**
     * @throws BusinessRuleException if the text has no voting section or a section has no totals
     */
    static List<ParsedVoting> parse(String rawText) {
        String text = PAGE_HEADER.matcher(rawText).replaceAll(" ").replaceAll("\\s+", " ").trim();
        List<ParsedVoting> result = new ArrayList<>();
        Matcher marker = MARKER.matcher(text);
        int previousEnd = 0;
        while (marker.find()) {
            String segment = text.substring(previousEnd, marker.start());
            String region = region(text, marker.end());
            int[] totals = totals(region, result.size() + 1);
            int counted = totals[0] + totals[1] + totals[2] + totals[3];
            int present = Math.max(present(segment, counted), counted);
            VoteResult outcome = outcome(text, marker.end(), totals);
            result.add(new ParsedVoting(question(segment, result.size() + 1), outcome, totals[0], totals[1],
                    totals[2], totals[3], present, named(region, totals)));
            previousEnd = marker.end();
        }
        if (result.isEmpty()) {
            throw new BusinessRuleException("У протоколі не знайдено розділу з результатами голосування "
                    + "(«Результати голосування: За — …, Проти — …»). Додайте його й завантажте протокол знову.");
        }
        return result;
    }

    private static Pattern names(String label) {
        return Pattern.compile(label + "\\s*[-—–:]\\s*\\d+\\s*:\\s*(.+?)" + NEXT_LABEL);
    }

    private static Pattern counter(String label) {
        return Pattern.compile(label + "\\s*[-—–:]\\s*(\\d+)|(\\d+)\\s*[-—–]\\s*" + label);
    }

    /**
     * Text after a marker up to the decision that follows the totals.
     */
    private static String region(String text, int from) {
        int to = Math.min(text.length(), from + MAX_REGION);
        Matcher end = REGION_END.matcher(text);
        if (end.find(from) && end.start() < to) {
            to = end.start();
        }
        Matcher next = MARKER.matcher(text);
        if (next.find(from) && next.start() < to) {
            to = next.start();
        }
        return text.substring(from, to);
    }

    private static int[] totals(String region, int number) {
        int[] totals = {
            find(FOR, region), find(AGAINST, region), find(ABSTAIN, region), find(ABSENT, region)
        };
        if (totals[0] < 0 && totals[1] < 0 && totals[2] < 0 && totals[3] < 0) {
            throw new BusinessRuleException("Не вдалося розпізнати кількість голосів у розділі «Результати "
                    + "голосування» №" + number + ". Вкажіть її у вигляді «За - 10», «Проти - 0», «Утримався - 0».");
        }
        for (int i = 0; i < totals.length; i++) {
            totals[i] = Math.max(totals[i], 0);
        }
        return totals;
    }

    private static Map<String, VoteChoice> named(String region, int[] totals) {
        Map<String, VoteChoice> named = new LinkedHashMap<>();
        VoteChoice[] choices = {VoteChoice.FOR, VoteChoice.AGAINST, VoteChoice.ABSTAIN, VoteChoice.ABSENT};
        Pattern[] labels = {FOR_NAMES, AGAINST_NAMES, ABSTAIN_NAMES, ABSENT_NAMES};
        for (int i = 0; i < choices.length; i++) {
            Matcher m = labels[i].matcher(region);
            if (!m.find()) {
                continue;
            }
            List<String> names = java.util.Arrays.stream(m.group(1).split("\\s*[;,]\\s*"))
                    .map(n -> n.replaceAll("[\\s.]+$", "")).filter(n -> !n.isBlank()).toList();
            if (names.size() != totals[i]) {
                throw new BusinessRuleException("У розділі «Результати голосування» вказано " + totals[i]
                        + " голосів «" + choices[i].name().toLowerCase(Locale.ROOT) + "», але імен перелічено "
                        + names.size());
            }
            for (String n : names) {
                named.put(n, choices[i]);
            }
        }
        return named;
    }

    private static int find(Pattern pattern, String region) {
        Matcher m = pattern.matcher(region);
        if (!m.find()) {
            return -1;
        }
        return Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
    }

    private static int present(String segment, int fallback) {
        Matcher m = PRESENT.matcher(segment);
        int present = fallback;
        while (m.find()) {
            present = Integer.parseInt(m.group(1));
        }
        return present;
    }

    /**
     * The decision the protocol states; when it is missing, a plain majority of the counted votes.
     */
    private static VoteResult outcome(String text, int from, int[] totals) {
        String tail = text.substring(from, Math.min(text.length(), from + MAX_REGION + 100));
        Matcher next = MARKER.matcher(tail);
        if (next.find()) {
            tail = tail.substring(0, next.start());
        }
        if (REJECTED.matcher(tail).find()) {
            return VoteResult.REJECTED;
        }
        if (ACCEPTED.matcher(tail).find()) {
            return VoteResult.ACCEPTED;
        }
        return totals[0] > totals[1] ? VoteResult.ACCEPTED : VoteResult.REJECTED;
    }

    private static String question(String segment, int number) {
        String question = null;
        Matcher put = PUT_TO_VOTE.matcher(segment);
        int after = -1;
        while (put.find()) {
            after = put.end();
        }
        if (after >= 0) {
            question = segment.substring(after).trim();
        } else {
            Matcher item = NUMBERED_ITEM.matcher(segment);
            int itemEnd = -1;
            while (item.find()) {
                itemEnd = item.end();
            }
            if (itemEnd >= 0) {
                Matcher sentence = FIRST_SENTENCE.matcher(segment.substring(itemEnd).trim());
                if (sentence.find()) {
                    question = fromListenedLine(sentence.group(1));
                }
            }
        }
        if (question == null || question.isBlank()) {
            return "Голосування " + number;
        }
        question = question.replaceAll("[\\s.;:]+$", "");
        if (question.length() > MAX_QUESTION) {
            question = question.substring(0, MAX_QUESTION - 1) + "…";
        }
        return capitalise(question);
    }

    /**
     * "Горобець Ганну щодо пункту про X" becomes "X".
     */
    private static String fromListenedLine(String sentence) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        int about = lower.indexOf("щодо ");
        String rest = about >= 0 && about < 120 ? sentence.substring(about + "щодо ".length()) : sentence;
        return ITEM_PREFIX.matcher(rest.trim()).replaceFirst("");
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
