package ua.edu.ukma.oss.web.support;

import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.notification.OutboxStatus;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteResult;

/**
 * Ukrainian display names and CSS classes for domain enums, kept out of the templates.
 */
public final class Labels {

    private Labels() {
    }

    public static String protocolStatus(ProtocolStatus status) {
        return switch (status) {
            case DRAFT -> "Чернетка";
            case APPROVED -> "Затверджено";
            case PUBLISHED -> "Опубліковано";
        };
    }

    public static String protocolCss(ProtocolStatus status) {
        return status.name().toLowerCase();
    }

    public static String meetingStatus(MeetingStatus status) {
        return switch (status) {
            case PLANNED -> "Заплановано";
            case HELD -> "Відбулося";
            case CANCELLED -> "Скасовано";
        };
    }

    public static String meetingCss(MeetingStatus status) {
        return switch (status) {
            case PLANNED -> "approved";
            case HELD -> "published";
            case CANCELLED -> "danger";
        };
    }

    public static String petitionStatus(PetitionStatus status) {
        return switch (status) {
            case SUBMITTED -> "Подано";
            case SCHEDULED -> "Заплановано до розгляду";
            case CONSIDERED -> "Розглянуто";
            case REJECTED -> "Відхилено";
        };
    }

    public static String petitionCss(PetitionStatus status) {
        return switch (status) {
            case SUBMITTED -> "draft";
            case SCHEDULED -> "approved";
            case CONSIDERED -> "published";
            case REJECTED -> "danger";
        };
    }

    public static String petitionLink(PetitionLink link) {
        return switch (link) {
            case SEPARATE_MEETING -> "окреме засідання";
            case AGENDA_ITEM -> "пункт порядку денного";
        };
    }

    public static String choice(VoteChoice choice) {
        return switch (choice) {
            case FOR -> "За";
            case AGAINST -> "Проти";
            case ABSTAIN -> "Утримався";
            case ABSENT -> "Відсутній";
        };
    }

    public static String voteResult(VoteResult result) {
        return result == null ? "Голоси не внесено" : switch (result) {
            case ACCEPTED -> "Рішення прийнято";
            case REJECTED -> "Рішення не прийнято";
        };
    }

    public static String messenger(Messenger messenger) {
        return switch (messenger) {
            case WHATSAPP -> "WhatsApp";
            case TELEGRAM -> "Telegram";
        };
    }

    public static String outboxStatus(OutboxStatus status) {
        return switch (status) {
            case PENDING -> "У черзі";
            case SENT -> "Надіслано";
            case FAILED -> "Не вдалося";
        };
    }

    public static String outboxCss(OutboxStatus status) {
        return switch (status) {
            case PENDING -> "draft";
            case SENT -> "published";
            case FAILED -> "danger";
        };
    }

    /**
     * Up to two capital letters for a seat label, e.g. "Іван Петренко" gives "ІП".
     */
    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (sb.length() == 2) {
                break;
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
        }
        return sb.toString();
    }
}
