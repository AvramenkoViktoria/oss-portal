package ua.edu.ukma.oss.demo;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserDto;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.petition.SubmitCmd;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolWriter;
import ua.edu.ukma.oss.voting.VotingService;

/**
 * Fills an empty database with two bodies, their members, meetings in every lifecycle stage,
 * protocols and petitions, so every screen of the portal has something to show.
 */
@Component
@Profile("demo")
class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final String PASSWORD = "demo12345";
    private static final String ADMIN_EMAIL = "admin@ukma.edu.ua";
    private static final String MARKER_EMAIL = "sk.1@ukma.edu.ua";

    private final UserService users;
    private final OrganizationService organizations;
    private final MeetingService meetings;
    private final PetitionService petitions;
    private final ProtocolReader protocolReader;
    private final ProtocolWriter protocolWriter;
    private final VotingService votings;

    DemoDataLoader(UserService users, OrganizationService organizations, MeetingService meetings,
                   PetitionService petitions, ProtocolReader protocolReader, ProtocolWriter protocolWriter,
                   VotingService votings) {
        this.users = users;
        this.organizations = organizations;
        this.meetings = meetings;
        this.petitions = petitions;
        this.protocolReader = protocolReader;
        this.protocolWriter = protocolWriter;
        this.votings = votings;
    }

    @Override
    public void run(ApplicationArguments args) {
        // The marker is a demo-only account: the admin may already exist through the first-admin bootstrap.
        if (users.all().stream().anyMatch(u -> u.email().equals(MARKER_EMAIL))) {
            addBulkMeetings();
            return;
        }
        Long adminId = users.all().stream().filter(u -> u.email().equals(ADMIN_EMAIL)).findFirst()
                .map(UserDto::id).orElseGet(() -> {
                    Long id = users.register(ADMIN_EMAIL, PASSWORD, "Адміністратор порталу").id();
                    users.grant(id, Role.ADMIN, null);
                    return id;
                });

        OrganizationDto sk = organizations.getByCode(OrgCode.SK);
        OrganizationDto ks = organizations.getByCode(OrgCode.KS);
        seat(sk, "sk", Role.SK_MEMBER, List.of(
                "Олена Коваленко|ФІ|Голова колегії", "Андрій Шевченко|ФПвН|Заступник голови",
                "Марія Бондаренко|ФГН|Секретар", "Іван Мельник|ФеН|Член колегії", "Софія Ткаченко|ФП|Член колегії"));
        seat(ks, "ks", Role.KS_DELEGATE, List.of(
                "Дмитро Кравченко|ФІ|Голова конгрегації", "Анна Олійник|ФПвН|Заступник голови",
                "Максим Лисенко|ФГН|Секретар", "Катерина Гончар|ФеН|Делегат", "Богдан Мороз|ФП|Делегат",
                "Юлія Савченко|ФІ|Делегат", "Тарас Романенко|ФГН|Делегат"));

        Long student1 = users.register("student1@ukma.edu.ua", PASSWORD, "Назар Студентенко").id();
        Long student2 = users.register("student2@ukma.edu.ua", PASSWORD, "Ірина Петренко").id();

        Instant now = Instant.now();
        Map<Long, Long> protocolOfMeeting = new HashMap<>();

        // SK: a published protocol, a draft waiting for approval, an upcoming meeting.
        PetitionDto considered = petitions.submit(new SubmitCmd(student1, sk.id(),
                "Подовжити години роботи бібліотеки", "Прошу розглянути продовження роботи бібліотеки до 22:00 у сесію."));
        MeetingDto skHeld = meetings.schedule(new ScheduleCmd(sk.id(), "Звичайне засідання СК: бюджет семестру",
                now.minus(21, ChronoUnit.DAYS), null));
        meetings.addAgendaItem(skHeld.id(), "Затвердження бюджету семестру");
        meetings.addAgendaItem(skHeld.id(), "Різне");
        petitions.attach(considered.id(), skHeld.id(), PetitionLink.AGENDA_ITEM);
        meetings.markHeld(skHeld.id());
        ProtocolDto published = protocolReader.byMeeting(skHeld.id()).orElseThrow();
        attach(published, "protocol-sk-budget.docx", protocolText("Засідання Студентської Колегії", 5,
                new Vote("Затвердження бюджету семестру.", List.of(SK[0], SK[1], SK[2]), List.of(SK[3]),
                        List.of(SK[4]), List.of(), true),
                new Vote("Підтримання клопотання про продовження роботи бібліотеки.", List.of(SK[0]), List.of(SK[1]),
                        List.of(SK[2]), List.of(SK[3], SK[4]), false)));
        approveAndPublish(published, adminId);

        MeetingDto skDraft = meetings.schedule(new ScheduleCmd(sk.id(), "Позачергове засідання СК: студентський простір",
                now.minus(4, ChronoUnit.DAYS), null));
        meetings.addAgendaItem(skDraft.id(), "Облаштування студентського простору");
        meetings.markHeld(skDraft.id());
        ProtocolDto draft = protocolReader.byMeeting(skDraft.id()).orElseThrow();
        attach(draft, "protocol-sk-draft.docx", protocolText("Засідання Студентської Колегії", 5,
                new Vote("Виділення коштів на облаштування студентського простору.",
                        List.of(SK[0], SK[1], SK[2], SK[3]), List.of(), List.of(SK[4]), List.of(), true)));

        PetitionDto scheduled = petitions.submit(new SubmitCmd(student2, sk.id(),
                "Облаштувати зони відпочинку у корпусах", "Пропоную встановити м’які зони відпочинку у 2 та 5 корпусах."));
        MeetingDto skUpcoming = meetings.schedule(new ScheduleCmd(sk.id(), "Звичайне засідання СК: наступний місяць",
                now.plus(5, ChronoUnit.DAYS), "https://stream.example.org/sk-live"));
        meetings.addAgendaItem(skUpcoming.id(), "Звіт про використання бюджету");
        petitions.attach(scheduled.id(), skUpcoming.id(), PetitionLink.AGENDA_ITEM);
        petitions.submit(new SubmitCmd(student1, sk.id(), "Спільний простір для настільних ігор",
                "Прошу виділити кімнату для студентського клубу настільних ігор."));
        PetitionDto rejected = petitions.submit(new SubmitCmd(student2, sk.id(), "Скасувати іспити",
                "Пропоную скасувати всі іспити у літній сесії."));
        petitions.reject(rejected.id(), "Питання не належить до компетенції колегії");

        // KS: one published protocol and an upcoming meeting.
        MeetingDto ksHeld = meetings.schedule(new ScheduleCmd(ks.id(), "Засідання конгрегації: зміни до регламенту",
                now.minus(14, ChronoUnit.DAYS), null));
        meetings.addAgendaItem(ksHeld.id(), "Зміни до регламенту");
        meetings.markHeld(ksHeld.id());
        ProtocolDto ksProtocol = protocolReader.byMeeting(ksHeld.id()).orElseThrow();
        attach(ksProtocol, "protocol-ks-regulations.docx", protocolText("Засідання Конгрегації студентів", 7,
                new Vote("Прийняття змін до регламенту в цілому.", List.of(KS[0], KS[1], KS[2], KS[3]),
                        List.of(KS[4]), List.of(KS[5]), List.of(KS[6]), true)));
        approveAndPublish(ksProtocol, adminId);
        MeetingDto ksUpcoming = meetings.schedule(new ScheduleCmd(ks.id(), "Засідання конгрегації: вибори голови",
                now.plus(9, ChronoUnit.DAYS), null));
        meetings.addAgendaItem(ksUpcoming.id(), "Вибори голови конгрегації");

        addBulkMeetings();
        log.info("Demo data loaded. Accounts (password {}): {}, sk.1@ukma.edu.ua (SK editor), "
                + "ks.1@ukma.edu.ua (KS editor), student1@ukma.edu.ua", PASSWORD, ADMIN_EMAIL);
    }

    /**
     * Fills the calendars of both bodies with meetings in every state, so the cabinets have something to browse:
     * past meetings with published, approved, draft and missing protocols, cancelled ones and a future schedule.
     * Runs once; bodies that were not seated by this loader are left alone.
     */
    private void addBulkMeetings() {
        OrganizationDto sk = organizations.getByCode(OrgCode.SK);
        OrganizationDto ks = organizations.getByCode(OrgCode.KS);
        if (meetings.byOrg(sk.id()).stream().anyMatch(m -> m.title().equals("Засідання СК №1"))
                || organizations.members(sk.id()).size() != SK.length) {
            return;
        }
        Long adminId = users.all().stream().filter(u -> u.email().equals(ADMIN_EMAIL)).findFirst()
                .map(UserDto::id).orElseThrow();
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        fillCalendar(sk, "СК", SK, adminId, today, new Random(26));
        fillCalendar(ks, "КС", KS, adminId, today, new Random(12));
        log.info("Demo meetings added for the SK and the KS calendars");
    }

    private static final List<String> TOPICS = List.of("Бюджет семестру", "Студентський простір", "Звіт відділів",
            "Гуртожитки", "Академічна доброчесність", "Навчальні програми", "Культурні заходи", "Волонтерство",
            "Спортивні заходи", "Зміни до положення", "Комунікація зі студентами", "Закупівлі");

    private void fillCalendar(OrganizationDto org, String abbr, String[] names, Long adminId, Instant today,
                              Random rnd) {
        // Offsets in days from today; negative are past. Some days hold two meetings.
        int[] past = {-150, -138, -124, -110, -96, -83, -70, -59, -47, -35, -29, -23, -23, -16, -9, -3};
        int[] future = {2, 6, 6, 11, 17, 24, 31, 38, 52, 66};
        int number = 1;
        for (int i = 0; i < past.length; i++) {
            String title = (i % 4 == 3 ? "Позачергове засідання " : "Засідання ") + abbr + " №" + number++;
            MeetingDto m = meetings.schedule(new ScheduleCmd(org.id(), title,
                    today.plus(past[i], ChronoUnit.DAYS).plus(10 + rnd.nextInt(8), ChronoUnit.HOURS), null));
            addAgenda(m, rnd);
            if (i % 9 == 8) {
                // A meeting that never took place.
                meetings.cancel(m.id());
                continue;
            }
            meetings.markHeld(m.id());
            ProtocolDto protocol = protocolReader.byMeeting(m.id()).orElseThrow();
            // The latest meetings are still in work, older ones are approved or published, one has no document.
            int age = past.length - 1 - i;
            if (age == 0) {
                continue;
            }
            attach(protocol, "protocol-" + protocol.id() + ".docx", protocolText(
                    org.code() == OrgCode.SK ? "Засідання Студентської Колегії" : "Засідання Конгрегації студентів",
                    names.length, randomVotes(names, rnd, 1 + rnd.nextInt(3))));
            if (age >= 3) {
                protocolWriter.approve(protocol.id(), adminId);
            }
            if (age >= 5) {
                protocolWriter.publish(protocol.id(), adminId);
            }
        }
        for (int day : future) {
            String title = "Засідання " + abbr + " №" + number++;
            MeetingDto m = meetings.schedule(new ScheduleCmd(org.id(), title,
                    today.plus(day, ChronoUnit.DAYS).plus(10 + rnd.nextInt(8), ChronoUnit.HOURS),
                    rnd.nextBoolean() ? "https://stream.example.org/" + abbr.toLowerCase() + "-" + number : null));
            addAgenda(m, rnd);
            if (day == 17) {
                meetings.cancel(m.id());
            }
        }
    }

    private void addAgenda(MeetingDto meeting, Random rnd) {
        for (int i = 0, items = 1 + rnd.nextInt(3); i < items; i++) {
            meetings.addAgendaItem(meeting.id(), TOPICS.get(rnd.nextInt(TOPICS.size())));
        }
    }

    private static Vote[] randomVotes(String[] names, Random rnd, int count) {
        Vote[] votes = new Vote[count];
        for (int v = 0; v < count; v++) {
            List<String> forVotes = new ArrayList<>();
            List<String> against = new ArrayList<>();
            List<String> abstain = new ArrayList<>();
            List<String> absent = new ArrayList<>();
            for (String name : names) {
                int roll = rnd.nextInt(10);
                (roll < 6 ? forVotes : roll < 8 ? against : roll < 9 ? abstain : absent).add(name);
            }
            votes[v] = new Vote(TOPICS.get(rnd.nextInt(TOPICS.size())) + ": ухвалити пропозицію.", forVotes, against,
                    abstain, absent, forVotes.size() * 2 > names.length);
        }
        return votes;
    }

    /**
     * Registers the people of one body; the first one also gets the editor role.
     */
    private List<MembershipDto> seat(OrganizationDto org, String prefix, Role role, List<String> people) {
        List<MembershipDto> seats = new ArrayList<>();
        for (int i = 0; i < people.size(); i++) {
            String[] parts = people.get(i).split("\\|");
            Long userId = users.register(prefix + "." + (i + 1) + "@ukma.edu.ua", PASSWORD, parts[0]).id();
            if (i == 0) {
                users.grant(userId, role, org.id());
            }
            seats.add(organizations.addMember(new AddMemberCmd(userId, org.id(), parts[1], parts[2],
                    LocalDate.now().minusMonths(6), null)));
        }
        return seats;
    }

    /**
     * One voting of a demo protocol, written the way real protocols state it.
     */
    private record Vote(String question, List<String> votesFor, List<String> votesAgainst,
                        List<String> votesAbstain, List<String> notVoted, boolean accepted) {
    }

    private static final String[] SK = {"Олена Коваленко", "Андрій Шевченко", "Марія Бондаренко", "Іван Мельник",
        "Софія Ткаченко"};
    private static final String[] KS = {"Дмитро Кравченко", "Анна Олійник", "Максим Лисенко", "Катерина Гончар",
        "Богдан Мороз", "Юлія Савченко", "Тарас Романенко"};

    private static String tally(String label, List<String> names) {
        return label + " - " + names.size() + (names.isEmpty() ? "" : ": " + String.join("; ", names));
    }

    private static List<String> protocolText(String title, int present, Vote... votes) {
        List<String> lines = new ArrayList<>(List.of("ПРОТОКОЛ", title, "Присутніх: " + present));
        for (Vote v : votes) {
            lines.addAll(List.of("Слухали: доповідь за пунктом порядку денного.", "Поставили на голосування:",
                    v.question(), "Результати голосування:", tally("За", v.votesFor()),
                    tally("Проти", v.votesAgainst()), tally("Утримався", v.votesAbstain()),
                    tally("Не голосували", v.notVoted()),
                    v.accepted() ? "Рішення прийнято" : "Рішення не прийнято"));
        }
        return lines;
    }

    private void attach(ProtocolDto protocol, String fileName, List<String> lines) {
        byte[] content = SampleDocx.of(lines);
        votings.importFromDocument(protocol.id(), fileName, content);
        protocolWriter.attachFile(protocol.id(), fileName, new ByteArrayInputStream(content));
    }

    private void approveAndPublish(ProtocolDto protocol, Long actorId) {
        protocolWriter.approve(protocol.id(), actorId);
        protocolWriter.publish(protocol.id(), actorId);
    }
}
