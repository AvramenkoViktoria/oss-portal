package ua.edu.ukma.oss.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserDto;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * Administration of accounts, roles and the composition of the bodies. Access is restricted to
 * administrators by the security configuration.
 */
@Controller
class AdminController {

    private final UserService users;
    private final OrganizationService organizations;
    private final WebAccess access;

    AdminController(UserService users, OrganizationService organizations, WebAccess access) {
        this.users = users;
        this.organizations = organizations;
        this.access = access;
    }

    @GetMapping("/admin")
    String users(Model model) {
        model.addAttribute("users", users.all());
        model.addAttribute("orgs", organizations.all());
        model.addAttribute("roles", List.of(Role.SK_MEMBER, Role.KS_DELEGATE, Role.ADMIN));
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/grant")
    String grant(@PathVariable Long id, @RequestParam Role role, @RequestParam(required = false) Long orgId,
                 RedirectAttributes redirect) {
        users.grant(id, role, role == Role.ADMIN ? null : orgId);
        redirect.addFlashAttribute("success", "Роль надано");
        return "redirect:/admin";
    }

    @PostMapping("/admin/grants/{grantId}/revoke")
    String revoke(@PathVariable Long grantId, @AuthenticationPrincipal AuthPrincipal admin,
                  RedirectAttributes redirect) {
        boolean ownAdminRole = users.get(admin.userId()).grants().stream()
                .anyMatch(g -> g.id().equals(grantId) && g.role() == Role.ADMIN);
        if (ownAdminRole) {
            throw new BusinessRuleException("Адміністратор не може відкликати власну роль адміністратора");
        }
        users.revoke(grantId);
        redirect.addFlashAttribute("success", "Роль відкликано");
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/active")
    String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
        users.setActive(id, active);
        redirect.addFlashAttribute("success", active ? "Обліковий запис увімкнено" : "Обліковий запис вимкнено");
        return "redirect:/admin";
    }

    @GetMapping("/admin/orgs/{code}")
    String composition(@PathVariable String code, Model model) {
        OrganizationDto org = access.org(code);
        List<MembershipDto> history = organizations.history(org.id());
        Map<Long, String> names = users.all().stream()
                .collect(Collectors.toMap(UserDto::id, UserDto::fullName));
        model.addAttribute("org", org);
        model.addAttribute("history", history);
        model.addAttribute("names", names);
        model.addAttribute("users", users.all());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("messengers", Messenger.values());
        return "admin/org";
    }

    @PostMapping("/admin/orgs/{code}/members")
    String addMember(@PathVariable String code, @RequestParam Long userId, @RequestParam String faculty,
                     @RequestParam String position, @RequestParam LocalDate termStart,
                     @RequestParam(required = false) LocalDate termEnd,
                     @RequestParam(defaultValue = "false") boolean grantEditor, RedirectAttributes redirect) {
        OrganizationDto org = access.org(code);
        if (faculty.isBlank() || position.isBlank()) {
            throw new BusinessRuleException("Факультет і посада обов’язкові");
        }
        organizations.addMember(new AddMemberCmd(userId, org.id(), faculty.trim(), position.trim(), termStart, termEnd));
        if (grantEditor) {
            users.grant(userId, editorRole(org), org.id());
        }
        redirect.addFlashAttribute("success", "Члена внесено до складу");
        return "redirect:/admin/orgs/" + code;
    }

    @PostMapping("/admin/orgs/{code}/members/{membershipId}/end")
    String endTerm(@PathVariable String code, @PathVariable Long membershipId, @RequestParam LocalDate termEnd,
                   RedirectAttributes redirect) {
        OrganizationDto org = access.org(code);
        MembershipDto membership = organizations.membership(membershipId);
        if (!membership.orgId().equals(org.id())) {
            throw new BusinessRuleException("Членство належить іншому органу");
        }
        organizations.endTerm(membershipId, termEnd);
        // The editor role follows the seat: once the term is over, editing rights go too.
        users.get(membership.userId()).grants().stream()
                .filter(g -> g.role() == editorRole(org) && org.id().equals(g.orgId()))
                .forEach(g -> users.revoke(g.id()));
        redirect.addFlashAttribute("success", "Каденцію завершено");
        return "redirect:/admin/orgs/" + code;
    }

    @PostMapping("/admin/orgs/{code}/channel")
    String channel(@PathVariable String code, @RequestParam Messenger messenger, @RequestParam String channelId,
                   RedirectAttributes redirect) {
        OrganizationDto org = access.org(code);
        if (channelId.isBlank()) {
            throw new BusinessRuleException("Вкажіть ідентифікатор каналу");
        }
        organizations.updateChannel(org.id(), messenger, channelId.trim());
        redirect.addFlashAttribute("success", "Канал оновлено");
        return "redirect:/admin/orgs/" + code;
    }

    private static Role editorRole(OrganizationDto org) {
        return org.code() == OrgCode.SK ? Role.SK_MEMBER : Role.KS_DELEGATE;
    }
}
