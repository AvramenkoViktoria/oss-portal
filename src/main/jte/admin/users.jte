@import java.util.List
@import ua.edu.ukma.oss.auth.Role
@import ua.edu.ukma.oss.auth.UserDto
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param List<UserDto> users
@param List<OrganizationDto> orgs
@param List<Role> roles
@template.layout.page(ctx = ctx, title = "Користувачі", section = "admin", content = @`
    @template.components.adminHead(active = "users")
    <div class="card table-wrap">
        <table class="data">
            <thead><tr><th>Користувач</th><th>Ролі</th><th>Надати роль</th><th></th></tr></thead>
            <tbody>
            @for(var u : users)
                <tr>
                    <td>
                        <strong>${u.fullName()}</strong>
                        <div class="muted small">${u.email()}</div>
                        @if(!u.active())<span class="chip danger">вимкнено</span>@endif
                    </td>
                    <td>
                        <span class="chip neutral">Студент</span>
                        @for(var g : u.grants())
                            <form class="inline-form" method="post" action="/admin/grants/${g.id()}/revoke">
                                <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                                <span class="chip approved">${g.role().name()}@if(g.orgId() != null) · ${orgs.stream().filter(o -> o.id().equals(g.orgId())).findFirst().map(o -> o.code().name()).orElse("")}@endif
                                    <button type="submit" title="Відкликати" style="border:0;background:none;cursor:pointer;color:inherit">×</button></span>
                            </form>
                        @endfor
                    </td>
                    <td>
                        <form method="post" action="/admin/users/${u.id()}/grant" class="row">
                            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                            <select name="role" style="width:auto">
                                @for(var r : roles)<option value="${r.name()}">${r.name()}</option>@endfor
                            </select>
                            <select name="orgId" style="width:auto">
                                @for(var o : orgs)<option value="${o.id()}">${o.code().name()}</option>@endfor
                            </select>
                            <button class="btn secondary small" type="submit">Надати</button>
                        </form>
                    </td>
                    <td class="right">
                        <form class="inline-form" method="post" action="/admin/users/${u.id()}/active">
                            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                            <input type="hidden" name="active" value="${!u.active()}">
                            <button class="btn ${u.active() ? "danger" : "secondary"} small" type="submit">${u.active() ? "Вимкнути" : "Увімкнути"}</button>
                        </form>
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`)
