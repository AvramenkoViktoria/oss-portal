package ua.edu.ukma.oss.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolFile;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.NotFoundException;
import ua.edu.ukma.oss.web.view.CalendarView;
import ua.edu.ukma.oss.web.view.MeetingPage;
import ua.edu.ukma.oss.web.view.ProtocolPage;

/**
 * The open part of the site: bodies, meetings and published protocols.
 */
@Controller
class PublicController {

    private final OrganizationService organizations;
    private final ProtocolReader protocols;
    private final SiteViews views;
    private final WebAccess access;

    PublicController(OrganizationService organizations, ProtocolReader protocols, SiteViews views,
                     WebAccess access) {
        this.organizations = organizations;
        this.protocols = protocols;
        this.views = views;
        this.access = access;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("summaries", organizations.all().stream().map(views::summary).toList());
        return "pages/home";
    }

    @GetMapping("/orgs/{code}")
    String org(@PathVariable String code, @RequestParam(required = false) String month,
               @RequestParam(defaultValue = "calendar") String view, Model model) {
        OrganizationDto org = access.org(code);
        model.addAttribute("page", views.orgPage(org, CalendarView.parseMonth(month, views.currentMonth())));
        model.addAttribute("view", "list".equals(view) ? "list" : "calendar");
        return "pages/org";
    }

    @GetMapping("/meetings/{id}")
    String meeting(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user, Model model) {
        MeetingPage page = views.meetingPage(id);
        model.addAttribute("page", page);
        model.addAttribute("canEdit", access.canEdit(user, page.meeting().orgId()));
        return "pages/meeting";
    }

    @GetMapping("/protocols/{id}")
    String protocol(@PathVariable Long id, Model model) {
        ProtocolDto protocol = protocols.get(id);
        if (protocol.status() != ProtocolStatus.PUBLISHED) {
            // Unpublished protocols are not public; do not reveal that they exist.
            throw new NotFoundException("Protocol", id);
        }
        ProtocolPage page = views.protocolPage(id, false);
        model.addAttribute("page", page);
        return "pages/protocol";
    }

    @GetMapping("/protocols/{id}/file")
    ResponseEntity<InputStreamResource> file(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user)
            throws IOException {
        ProtocolDto protocol = protocols.get(id);
        if (protocol.status() != ProtocolStatus.PUBLISHED && !access.canEdit(user, protocol.orgId())) {
            throw new NotFoundException("Protocol", id);
        }
        ProtocolFile file = protocols.file(id).orElseThrow(() -> new NotFoundException("Protocol file", id));
        InputStream content = file.content();
        MediaType type = file.fileName().toLowerCase().endsWith(".pdf")
                ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(content));
    }
}
