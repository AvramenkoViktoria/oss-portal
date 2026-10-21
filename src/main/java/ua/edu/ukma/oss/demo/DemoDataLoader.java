package ua.edu.ukma.oss.demo;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ua.edu.ukma.oss.voting.CreateVotingCmd;
import ua.edu.ukma.oss.voting.RecordCmd;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VotingDto;
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
        List<MembershipDto> skSeats = seat(sk, "sk", Role.SK_MEMBER, List.of(
                "Олена Коваленко|ФІ|Голова колегії", "Андрій Шевченко|ФПвН|Заступник голови",
                "Марія Бондаренко|ФГН|Секретар", "Іван Мельник|ФеН|Член колегії", "Софія Ткаченко|ФП|Член колегії"));
        List<MembershipDto> ksSeats = seat(ks, "ks", Role.KS_DELEGATE, List.of(
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
        vote(published.id(), skSeats, "Затвердити бюджет семестру?", List.of(
                VoteChoice.FOR, VoteChoice.FOR, VoteChoice.FOR, VoteChoice.AGAINST, VoteChoice.ABSTAIN));
        vote(published.id(), skSeats, "Підтримати клопотання про продовження роботи бібліотеки?", List.of(
                VoteChoice.FOR, VoteChoice.AGAINST, VoteChoice.ABSTAIN, VoteChoice.ABSENT, VoteChoice.ABSENT));
        publish(published, "Протокол засідання СК: бюджет семестру", adminId);

        MeetingDto skDraft = meetings.schedule(new ScheduleCmd(sk.id(), "Позачергове засідання СК: студентський простір",
                now.minus(4, ChronoUnit.DAYS), null));
        meetings.addAgendaItem(skDraft.id(), "Облаштування студентського простору");
        meetings.markHeld(skDraft.id());
        ProtocolDto draft = protocolReader.byMeeting(skDraft.id()).orElseThrow();
        protocolWriter.attachFile(draft.id(), "protocol-sk-draft.pdf", pdf("Draft protocol of the SK meeting"));
        votings.create(new CreateVotingCmd(draft.id(), null, "Виділити кошти на облаштування студентського простору?"));

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
        vote(ksProtocol.id(), ksSeats, "Прийняти зміни до регламенту в цілому?", List.of(
                VoteChoice.FOR, VoteChoice.FOR, VoteChoice.FOR, VoteChoice.FOR, VoteChoice.AGAINST,
                VoteChoice.ABSTAIN, VoteChoice.ABSENT));
        publish(ksProtocol, "Protocol of the KS meeting", adminId);
        MeetingDto ksUpcoming = meetings.schedule(new ScheduleCmd(ks.id(), "Засідання конгрегації: вибори голови",
                now.plus(9, ChronoUnit.DAYS), null));
        meetings.addAgendaItem(ksUpcoming.id(), "Вибори голови конгрегації");

        log.info("Demo data loaded. Accounts (password {}): {}, sk.1@ukma.edu.ua (SK editor), "
                + "ks.1@ukma.edu.ua (KS editor), student1@ukma.edu.ua", PASSWORD, ADMIN_EMAIL);
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

    private void vote(Long protocolId, List<MembershipDto> seats, String question, List<VoteChoice> choices) {
        VotingDto voting = votings.create(new CreateVotingCmd(protocolId, null, question));
        Map<Long, VoteChoice> byMember = new HashMap<>();
        for (int i = 0; i < seats.size(); i++) {
            byMember.put(seats.get(i).id(), choices.get(i));
        }
        votings.record(new RecordCmd(voting.id(), byMember));
    }

    private void publish(ProtocolDto protocol, String pdfTitle, Long actorId) {
        protocolWriter.attachFile(protocol.id(), "protocol-" + protocol.id() + ".pdf", pdf(pdfTitle));
        protocolWriter.approve(protocol.id(), actorId);
        protocolWriter.publish(protocol.id(), actorId);
    }

    private static ByteArrayInputStream pdf(String title) {
        return new ByteArrayInputStream(SamplePdf.of(title));
    }
}
