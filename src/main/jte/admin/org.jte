@import java.time.LocalDate
@import java.util.List
@import java.util.Map
@import ua.edu.ukma.oss.auth.UserDto
@import ua.edu.ukma.oss.organization.MembershipDto
@import ua.edu.ukma.oss.organization.Messenger
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param OrganizationDto org
@param List<MembershipDto> history
@param Map<Long, String> names
@param List<UserDto> users
@param LocalDate today
@param Messenger[] messengers
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Склад " + org.code().name(), section = "admin", content = @`
    @template.components.adminHead(active = code)
    <div class="split">
        <div>
            <section class="card">
                <h2>Склад і каденції</h2>
                @if(history.isEmpty())
                    <p class="muted">Склад порожній.</p>
                @else
                    <div class="table-wrap">
                        <table class="data">
                            <thead><tr><th>Ім’я</th><th>Посада</th><th>Каденція</th><th></th></tr></thead>
                            <tbody>
                            @for(var m : history)
                                !{boolean active = m.isActiveOn(today);}
                                <tr>
                                    <td>${names.getOrDefault(m.userId(), "—")}<div class="muted small">${m.faculty()}</div></td>
                                    <td>${m.position()}</td>
                                    <td>
                                        ${ctx.fmt().date(m.termStart())} — @if(m.termEnd() != null)${ctx.fmt().date(m.termEnd())}@else<span class="muted">за чинністю</span>@endif
                                        @if(active)<br><span class="chip published">діє</span>@endif
                                    </td>
                                    <td class="right">
                                        @if(active)
                                            <form class="inline-form" method="post" action="/admin/orgs/${code}/members/${m.id()}/end">
                                                <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                                                <input type="hidden" name="termEnd" value="${today.toString()}">
                                                <button class="btn danger small" type="submit">Завершити</button>
                                            </form>
                                        @endif
                                    </td>
                                </tr>
                            @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
            </section>
            <section class="card">
                <h2>Додати члена</h2>
                <form method="post" action="/admin/orgs/${code}/members">
                    <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                    <div class="field">
                        <label for="userId">Користувач</label>
                        <select id="userId" name="userId" required>
                            @for(var u : users)<option value="${u.id()}">${u.fullName()} (${u.email()})</option>@endfor
                        </select>
                    </div>
                    <div class="grid two">
                        <div class="field"><label for="faculty">Факультет</label><input id="faculty" name="faculty" type="text" required></div>
                        <div class="field"><label for="position">Посада</label><input id="position" name="position" type="text" required></div>
                        <div class="field"><label for="termStart">Початок каденції</label><input id="termStart" name="termStart" type="date" value="${today.toString()}" required></div>
                        <div class="field"><label for="termEnd">Кінець каденції (необов’язково)</label><input id="termEnd" name="termEnd" type="date"></div>
                    </div>
                    <div class="field"><label><input type="checkbox" name="grantEditor" value="true" style="width:auto"> Надати права редактора цього органу</label></div>
                    <button class="btn" type="submit">Додати</button>
                </form>
            </section>
        </div>
        <aside>
            <section class="card">
                <h3>Канал розсилки</h3>
                <form method="post" action="/admin/orgs/${code}/channel">
                    <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                    <div class="field">
                        <label for="messenger">Месенджер</label>
                        <select id="messenger" name="messenger">
                            @for(var m : messengers)
                                <option value="${m.name()}" selected="${m == org.messenger()}">${m.name()}</option>
                            @endfor
                        </select>
                    </div>
                    <div class="field"><label for="channelId">Ідентифікатор каналу</label><input id="channelId" name="channelId" type="text" value="${org.channelId()}" required></div>
                    <button class="btn secondary" type="submit">Зберегти</button>
                </form>
            </section>
        </aside>
    </div>
`)
